package com.example.spring.controller;

import com.example.spring.config.AppProperties;
import com.example.spring.entity.*;
import com.example.spring.common.exception.BadRequestException;
import com.example.spring.common.exception.ForbiddenException;
import com.example.spring.common.exception.NotFoundException;
import com.example.spring.repository.EnrollmentRepository;
import com.example.spring.repository.LectureVideoRepository;
import com.example.spring.repository.UserRepository;
import com.example.spring.security.CurrentUser;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoStreamController {

    private static final long CHUNK_SIZE = 1024 * 1024; // 1MB

    private final LectureVideoRepository lectureVideoRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final Path baseDir;
    private final long chunkSize;

    public VideoStreamController(
            LectureVideoRepository lectureVideoRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            AppProperties props
    ) {
        this.lectureVideoRepository = lectureVideoRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.baseDir = Paths.get(props.getUpload().getBaseDir()).toAbsolutePath().normalize();
        this.chunkSize = props.getUpload().getChunkSizeBytes();
    }

    /**
     * 업로드 영상 스트리밍
     * - ADMIN: 모두 가능
     * - PROFESSOR: 본인 강의만 가능
     * - USER: 수강(Enrollment)한 강의만 가능
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<?> stream(
            Authentication authentication,
            @PathVariable Long videoId,
            @RequestHeader HttpHeaders headers
    ) {
        Long userId = CurrentUser.getUserId(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        LectureVideo video = lectureVideoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException("비디오를 찾을 수 없습니다."));

        if (video.getSourceType() != VideoSourceType.UPLOAD) {
            throw new BadRequestException("로컬 업로드 영상만 스트리밍할 수 있습니다.");
        }

        Lecture lecture = video.getLecture();
        if (lecture == null) {
            throw new NotFoundException("비디오에 연결된 강의를 찾을 수 없습니다.");
        }

        // 권한 체크
        authorizeVideoAccess(user, lecture);

        // 파일 경로 resolve + 스트리밍
        String localPath = video.getLocalPath();
        if (!StringUtils.hasText(localPath)) {
            throw new NotFoundException("서버에 저장된 파일 경로가 없습니다.");
        }

        String relative = localPath.startsWith("/") ? localPath.substring(1) : localPath;
        Path filePath = baseDir.resolve(relative).normalize();

        // 경로 조작 방지
        if (!filePath.startsWith(baseDir)) {
            throw new BadRequestException("잘못된 파일 경로입니다.");
        }
        if (!Files.exists(filePath)) {
            throw new NotFoundException("파일이 존재하지 않습니다.");
        }

        Resource resource = toResource(filePath);
        long contentLength = contentLength(filePath);
        MediaType mediaType = resolveMediaType(video.getMimeType(), filePath);

        List<HttpRange> ranges = headers.getRange();
        if (ranges == null || ranges.isEmpty()) {
            // Range 요청이 없으면 전체 반환(200)
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(contentLength)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        }

        // Range 요청(대부분 1개)
        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(contentLength);
        long end = range.getRangeEnd(contentLength);

        long rangeLength = Math.min(chunkSize, end - start + 1);
        ResourceRegion region = new ResourceRegion(resource, start, rangeLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(region);
    }

    private void authorizeVideoAccess(User user, Lecture lecture) {
        UserRole role = UserRole.fromCode(user.getUserRole());

        // ADMIN: 전부 OK
        if (role == UserRole.ADMIN) return;

        // PROFESSOR: 본인 강의만 OK
        if (role == UserRole.PROFESSOR) {
            if (lecture.getProfessor() == null || lecture.getProfessor().getUserId() == null) {
                throw new ForbiddenException("강의 교수 정보가 없어 접근할 수 없습니다.");
            }
            if (!lecture.getProfessor().getUserId().equals(user.getUserId())) {
                throw new ForbiddenException("본인 강의 영상만 접근할 수 있습니다.");
            }
            return;
        }

        // USER(학생): 수강 신청한 강의만 OK
        boolean enrolled = enrollmentRepository.existsByUser_UserIdAndLecture_LectureId(
                user.getUserId(),
                lecture.getLectureId()
        );
        if (!enrolled) {
            throw new ForbiddenException("수강 신청한 강의만 영상을 볼 수 있습니다.");
        }

        // (선택) 승인된 강의만 보여주고 싶으면 아래 추가 가능
        // if (lecture.getStatus() != LectureStatus.APPROVED) throw new ForbiddenException("승인된 강의만 접근 가능합니다.");
    }

    private Resource toResource(Path filePath) {
        try {
            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new BadRequestException("리소스 변환 실패: " + e.getMessage());
        }
    }

    private long contentLength(Path filePath) {
        try {
            return Files.size(filePath);
        } catch (Exception e) {
            throw new BadRequestException("파일 크기 조회 실패: " + e.getMessage());
        }
    }

    private MediaType resolveMediaType(String mimeType, Path filePath) {
        try {
            if (StringUtils.hasText(mimeType)) return MediaType.parseMediaType(mimeType);
        } catch (Exception ignored) {}

        try {
            String probed = Files.probeContentType(filePath);
            if (StringUtils.hasText(probed)) return MediaType.parseMediaType(probed);
        } catch (Exception ignored) {}

        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
