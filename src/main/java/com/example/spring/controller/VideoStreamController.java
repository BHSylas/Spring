package com.example.spring.controller;

import com.example.spring.config.AppProperties;
import com.example.spring.entity.*;
import com.example.spring.common.exception.BadRequestException;
import com.example.spring.common.exception.ForbiddenException;
import com.example.spring.common.exception.NotFoundException;
import com.example.spring.repository.EnrollmentRepository;
import com.example.spring.repository.LectureVideoRepository;
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

    private final LectureVideoRepository lectureVideoRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final Path baseDir;
    private final long chunkSize;

    public VideoStreamController(
            LectureVideoRepository lectureVideoRepository,
            EnrollmentRepository enrollmentRepository,
            AppProperties props
    ) {
        this.lectureVideoRepository = lectureVideoRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.baseDir = Paths.get(props.getUpload().getBaseDir()).toAbsolutePath().normalize();
        this.chunkSize = props.getUpload().getChunkSizeBytes();
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<?> stream(
            Authentication authentication,
            @PathVariable Long videoId,
            @RequestHeader HttpHeaders headers
    ) {
        Long userId = CurrentUser.getUserId(authentication);

        // lecture/professor까지 한번에 로딩
        LectureVideo video = lectureVideoRepository.findWithLectureAndProfessorByVideoId(videoId)
                .orElseThrow(() -> new NotFoundException("비디오를 찾을 수 없습니다."));

        if (video.getSourceType() != VideoSourceType.UPLOAD) {
            throw new BadRequestException("로컬 업로드 영상만 스트리밍할 수 있습니다.");
        }

        Lecture lecture = video.getLecture();
        if (lecture == null) {
            throw new NotFoundException("비디오에 연결된 강의를 찾을 수 없습니다.");
        }

        // 권한 체크(토큰 authority 사용 + exists 쿼리)
        authorizeVideoAccess(authentication, userId, lecture);

        String localPath = video.getLocalPath();
        if (!StringUtils.hasText(localPath)) {
            throw new NotFoundException("서버에 저장된 파일 경로가 없습니다.");
        }

        String relative = localPath.startsWith("/") ? localPath.substring(1) : localPath;
        Path filePath = baseDir.resolve(relative).normalize();

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
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(contentLength)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        }

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

    private void authorizeVideoAccess(Authentication authentication, Long userId, Lecture lecture) {
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        boolean isProfessor = hasRole(authentication, "ROLE_PROFESSOR");
        boolean isUser = hasRole(authentication, "ROLE_USER");

        if (isAdmin) return;

        if (isProfessor) {
            if (lecture.getProfessor() == null || lecture.getProfessor().getUserId() == null) {
                throw new ForbiddenException("강의 교수 정보가 없어 접근할 수 없습니다.");
            }
            if (!lecture.getProfessor().getUserId().equals(userId)) {
                throw new ForbiddenException("본인 강의 영상만 접근할 수 있습니다.");
            }
            return;
        }

        if (!isUser) {
            throw new ForbiddenException("접근 권한이 없습니다.");
        }

        boolean enrolled = enrollmentRepository.existsByUser_UserIdAndLecture_LectureId(
                userId,
                lecture.getLectureId()
        );
        if (!enrolled) {
            throw new ForbiddenException("수강 신청한 강의만 영상을 볼 수 있습니다.");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
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
