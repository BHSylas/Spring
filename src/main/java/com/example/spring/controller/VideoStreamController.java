package com.example.spring.controller;

import com.example.spring.common.exception.BadRequestException;
import com.example.spring.common.exception.ForbiddenException;
import com.example.spring.common.exception.NotFoundException;
import com.example.spring.config.AppProperties;
import com.example.spring.entity.*;
import com.example.spring.repository.EnrollmentRepository;
import com.example.spring.repository.LectureVideoRepository;
import com.example.spring.security.CurrentUser;
import com.example.spring.security.VideoSignedUrlService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
    private final VideoSignedUrlService videoSignedUrlService;

    private final Path baseDir;
    private final long chunkSize;

    public VideoStreamController(
            LectureVideoRepository lectureVideoRepository,
            EnrollmentRepository enrollmentRepository,
            VideoSignedUrlService videoSignedUrlService,
            AppProperties props
    ) {
        this.lectureVideoRepository = lectureVideoRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.videoSignedUrlService = videoSignedUrlService;
        this.baseDir = Paths.get(props.getUpload().getBaseDir()).toAbsolutePath().normalize();
        this.chunkSize = props.getUpload().getChunkSizeBytes();
    }

    // =========================================================
    // 1) 재생 URL 발급 (JWT 인증 필요)
    // =========================================================
    @GetMapping("/{videoId}/play-url")
    public VideoPlayUrlResponse issuePlayUrl(
            Authentication authentication,
            @PathVariable Long videoId
    ) {
        Long userId = CurrentUser.getUserId(authentication);

        LectureVideo video = lectureVideoRepository.findWithLectureAndProfessorByVideoId(videoId)
                .orElseThrow(() -> new NotFoundException("비디오를 찾을 수 없습니다."));

        if (video.getSourceType() != VideoSourceType.UPLOAD) {
            throw new BadRequestException("로컬 업로드 영상만 스트리밍할 수 있습니다.");
        }

        Lecture lecture = video.getLecture();
        if (lecture == null) {
            throw new NotFoundException("비디오에 연결된 강의를 찾을 수 없습니다.");
        }

        // 기존 JWT + 권한 체크 재사용
        authorizeVideoAccess(authentication, userId, lecture);

        String role = resolvePrimaryRole(authentication); // ADMIN / PROFESSOR / USER
        long expires = videoSignedUrlService.computeExpiresEpochSeconds();
        String sig = videoSignedUrlService.createSignature(videoId, userId, role, expires);

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/videos/{videoId}")
                .queryParam("expires", expires)
                .queryParam("uid", userId)
                .queryParam("role", role)
                .queryParam("sig", sig)
                .buildAndExpand(videoId)
                .toUriString();

        return new VideoPlayUrlResponse(videoId, expires, url);
    }

    // =========================================================
    // 2) 스트리밍 (signed URL 검증 기반)
    // =========================================================
    @GetMapping("/{videoId}")
    public ResponseEntity<?> stream(
            @PathVariable Long videoId,
            @RequestHeader HttpHeaders headers,
            @RequestParam Long uid,
            @RequestParam String role,
            @RequestParam long expires,
            @RequestParam String sig
    ) {
        // 1) signed url 검증
        videoSignedUrlService.validateOrThrow(videoId, uid, role, expires, sig);

        // 2) 비디오 조회
        LectureVideo video = lectureVideoRepository.findWithLectureAndProfessorByVideoId(videoId)
                .orElseThrow(() -> new NotFoundException("비디오를 찾을 수 없습니다."));

        if (video.getSourceType() != VideoSourceType.UPLOAD) {
            throw new BadRequestException("로컬 업로드 영상만 스트리밍할 수 있습니다.");
        }

        Lecture lecture = video.getLecture();
        if (lecture == null) {
            throw new NotFoundException("비디오에 연결된 강의를 찾을 수 없습니다.");
        }

        // 3) Authentication 없이 uid/role 기반 권한 체크
        authorizeVideoAccessBySignedRole(uid, role, lecture);

        // 4) 파일 경로 검증 + path traversal 방지
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
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
                    .body(resource);
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(contentLength)
                .body(resource);

    }

    // =========================================================
    // 권한 체크
    // =========================================================

    // 기존 JWT 인증 기반 체크 (play-url 발급용)
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

    // signed URL 기반 체크 (stream용)
    private void authorizeVideoAccessBySignedRole(Long userId, String role, Lecture lecture) {
        String normalized = role == null ? "" : role.trim().toUpperCase();

        switch (normalized) {
            case "ADMIN" -> {
                return;
            }
            case "PROFESSOR" -> {
                if (lecture.getProfessor() == null || lecture.getProfessor().getUserId() == null) {
                    throw new ForbiddenException("강의 교수 정보가 없어 접근할 수 없습니다.");
                }
                if (!lecture.getProfessor().getUserId().equals(userId)) {
                    throw new ForbiddenException("본인 강의 영상만 접근할 수 있습니다.");
                }
            }
            case "USER" -> {
                boolean enrolled = enrollmentRepository.existsByUser_UserIdAndLecture_LectureId(
                        userId, lecture.getLectureId()
                );
                if (!enrolled) {
                    throw new ForbiddenException("수강 신청한 강의만 영상을 볼 수 있습니다.");
                }
            }
            default -> throw new ForbiddenException("유효하지 않은 권한 정보입니다.");
        }
    }

    private String resolvePrimaryRole(Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) return "ADMIN";
        if (hasRole(authentication, "ROLE_PROFESSOR")) return "PROFESSOR";
        if (hasRole(authentication, "ROLE_USER")) return "USER";
        throw new ForbiddenException("접근 권한이 없습니다.");
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

    // play-url 응답 DTO (컨트롤러 내부 record로 둬도 됨)
    public record VideoPlayUrlResponse(Long videoId, long expires, String url) {}
}