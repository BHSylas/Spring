package com.example.spring.service;

import com.example.spring.common.exception.BadRequestException;
import com.example.spring.common.exception.ForbiddenException;
import com.example.spring.common.exception.NotFoundException;
import com.example.spring.dto.*;
import com.example.spring.entity.*;
import com.example.spring.repository.EnrollmentRepository;
import com.example.spring.repository.LectureRepository;
import com.example.spring.repository.LectureVideoRepository;
import com.example.spring.repository.UserRepository;
import com.example.spring.security.RoleGuard;
import com.example.spring.storage.LocalFileStorage;
import com.example.spring.youtube.YoutubeClient;
import com.example.spring.youtube.YoutubeParser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LectureService {

    private final UserRepository userRepository;
    private final LectureRepository lectureRepository;
    private final LectureVideoRepository lectureVideoRepository;
    private final EnrollmentRepository enrollmentRepository;

    private final LocalFileStorage localFileStorage;
    private final YoutubeClient youtubeClient;

    // =========================================================
    // 강사: 강의 CRUD
    // =========================================================

    @Transactional
    public LectureResponseDTO createLecture(Long currentUserId, LectureCreateRequestDTO req) {
        User professor = requireProfessorUser(currentUserId);

        Lecture lecture = Lecture.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .country(req.getCountry())
                .language(req.getLanguage())
                .professor(professor)
                .build();

        return toLectureResponse(lectureRepository.save(lecture));
    }

    @Transactional
    public LectureResponseDTO updateLecture(Long currentUserId, Long lectureId, LectureUpdateRequestDTO req) {
        Lecture lecture = findLectureOrThrow(lectureId);
        requireLectureOwner(lecture, currentUserId);

        lecture.updateInfo(req.getTitle(), req.getDescription(), req.getCountry(), req.getLanguage());
        return toLectureResponse(lecture);
    }

    /**
     * 교수: 본인 강의 목록 (status=ALL이면 전체)
     */
    public Page<LectureResponseDTO> listMyLectures(Long currentUserId, String status, Pageable pageable) {
        requireProfessorUser(currentUserId);

        boolean all = isAll(status);
        Page<Lecture> page = all
                ? lectureRepository.findByProfessor_UserId(currentUserId, pageable)
                : lectureRepository.findByProfessor_UserIdAndStatus(
                currentUserId,
                parseLectureStatus(status, "status 값이 올바르지 않습니다. (ALL, PENDING, APPROVED, REJECTED)"),
                pageable
        );
        return page.map(this::toLectureResponse);
    }

    /**
     * 강사/관리자용 강의 통계 조회
     */
    public InstructorLectureStatsDTO getLectureStats(Long currentUserId, Long lectureId) {
        User caller = findUserOrThrow(currentUserId);
        UserRole role = UserRole.fromCode(caller.getUserRole());

        requireProfessorOrAdmin(role);

        Lecture lecture = findLectureOrThrow(lectureId);
        if (role == UserRole.PROFESSOR) {
            requireLectureOwner(lecture, currentUserId);
        }

        long enrolledCount = enrollmentRepository.countByLecture_LectureIdAndStatusNot(
                lectureId, EnrollmentStatus.CANCELED
        );

        long completedCount = enrollmentRepository.countByLecture_LectureIdAndStatus(
                lectureId, EnrollmentStatus.COMPLETED
        );

        double avg = enrollmentRepository.avgProgressRateExcludeStatus(
                lectureId, EnrollmentStatus.CANCELED
        );

        return new InstructorLectureStatsDTO(lectureId, enrolledCount, completedCount, avg);
    }

    // =========================================================
    // 관리자: 강의 승인/반려 + 목록
    // =========================================================

    public Page<LectureResponseDTO> adminListLectures(Long adminUserId, String status, Pageable pageable) {
        requireAdminUser(adminUserId);

        String st = (status == null || status.isBlank()) ? "PENDING" : status;
        if (isAll(st)) {
            return lectureRepository.findAll(pageable).map(this::toLectureResponse);
        }

        LectureStatus parsed = parseLectureStatus(st, "status 값이 올바르지 않습니다. (ALL, PENDING, APPROVED, REJECTED)");
        return lectureRepository.findByStatus(parsed, pageable).map(this::toLectureResponse);
    }

    @Transactional
    public LectureResponseDTO approveLecture(Long adminUserId, Long lectureId) {
        User admin = requireAdminUser(adminUserId);

        Lecture lecture = findLectureOrThrow(lectureId);
        lecture.approve(admin);

        return toLectureResponse(lecture);
    }

    @Transactional
    public LectureResponseDTO rejectLecture(Long adminUserId, Long lectureId, String reason) {
        User admin = requireAdminUser(adminUserId);

        Lecture lecture = findLectureOrThrow(lectureId);
        lecture.reject(admin, reason);

        return toLectureResponse(lecture);
    }

    // =========================================================
    // 학생: 공개 강의 목록/검색 (APPROVED 전용)
    // =========================================================

    public Page<LectureResponseDTO> listApprovedWithFilters(
            Long currentUserId,
            String language,
            Boolean enrolling,
            String keyword,
            Pageable pageable
    ) {
        String normLang = normalizeLanguage(language);
        String normKeyword = normalizeKeyword(keyword);

        // enrolling 필터 없으면 기존 검색 그대로
        if (enrolling == null) {
            return lectureRepository.searchApproved(LectureStatus.APPROVED, normLang, normKeyword, pageable)
                    .map(this::toLectureResponse);
        }

        // 로그인 없이 enrolling 쓰면 의미가 없음
        requireLoggedInForEnrollingFilter(currentUserId);

        var activeLectureIds = enrollmentRepository.findActiveLectureIdsByUserId(
                currentUserId, EnrollmentStatus.CANCELED
        );

        if (activeLectureIds.isEmpty()) {
            // enrolling=true면 빈 페이지 / enrolling=false면 전체 = 미수강 전체
            return enrolling
                    ? Page.empty(pageable)
                    : lectureRepository.searchApproved(LectureStatus.APPROVED, normLang, normKeyword, pageable)
                    .map(this::toLectureResponse);
        }

        Page<Lecture> page = enrolling
                ? lectureRepository.searchApprovedInLectureIds(LectureStatus.APPROVED, normLang, normKeyword, activeLectureIds, pageable)
                : lectureRepository.searchApprovedNotInLectureIds(LectureStatus.APPROVED, normLang, normKeyword, activeLectureIds, pageable);

        return page.map(this::toLectureResponse);
    }

    /**
     * 기존 메서드(호환용): 승인 강의만 단순 조회
     */
    @Transactional
    public Page<LectureResponseDTO> listApproved(Pageable pageable) {
        return lectureRepository.findByStatus(LectureStatus.APPROVED, pageable).map(this::toLectureResponse);
    }

    @Transactional
    public LectureResponseDTO getLectureDetail(Long lectureId) {
        return toLectureResponse(findLectureOrThrow(lectureId));
    }

    // =========================================================
    // 강의 영상: 업로드 / 유튜브 연결 / 메타 갱신
    // =========================================================

    @Transactional
    public LectureVideoResponseDTO uploadLectureVideo(Long currentUserId, Long lectureId, MultipartFile file) {
        Lecture lecture = findLectureOrThrow(lectureId);
        requireLectureOwner(lecture, currentUserId);
        requireNoExistingVideo(lectureId);

        var stored = localFileStorage.saveVideo(file);

        LectureVideo video = LectureVideo.ofUpload(
                lecture,
                stored.localPath(),
                stored.originalFilename(),
                stored.storedFilename(),
                stored.mimeType(),
                stored.fileSizeBytes(),
                0,
                null
        );

        return toVideoResponse(lectureVideoRepository.save(video));
    }

    @Transactional
    public LectureVideoResponseDTO attachYoutube(Long currentUserId, Long lectureId, YoutubeAttachRequestDTO req) {
        Lecture lecture = findLectureOrThrow(lectureId);
        requireLectureOwner(lecture, currentUserId);
        requireNoExistingVideo(lectureId);

        String videoId = extractYoutubeVideoIdOrThrow(req.getYoutubeUrlOrId());
        YoutubeClient.YoutubeMeta meta = youtubeClient.fetchMetaSafe(videoId);

        LectureVideo video = LectureVideo.ofYoutube(
                lecture,
                videoId,
                YoutubeParser.toWatchUrl(videoId),
                meta.channelTitle(),
                meta.durationSec(),
                meta.thumbnailUrl()
        );

        return toVideoResponse(lectureVideoRepository.save(video));
    }

    public Optional<LectureVideoResponseDTO> getLectureVideo(Long lectureId) {
        return lectureVideoRepository.findByLecture_LectureId(lectureId).map(this::toVideoResponse);
    }

    @Transactional
    public VideoMetaRefreshResponseDTO refreshYoutubeMeta(Long currentUserId, Long lectureId) {
        Lecture lecture = findLectureOrThrow(lectureId);

        User caller = findUserOrThrow(currentUserId);
        boolean isAdmin = UserRole.fromCode(caller.getUserRole()) == UserRole.ADMIN;

        if (!isAdmin) {
            requireLectureOwner(lecture, currentUserId);
        }

        LectureVideo video = lectureVideoRepository.findByLecture_LectureId(lectureId)
                .orElseThrow(() -> new NotFoundException("강의에 등록된 영상이 없습니다."));

        requireYoutubeVideo(video);

        String videoId = requireNonBlank(video.getYoutubeVideoId(), "youtubeVideoId가 비어있습니다.");

        YoutubeClient.YoutubeMeta meta = youtubeClient.fetchMetaSafe(videoId);

        // 메타가 비어있으면(키 없음/외부 실패/타임아웃) DB 업데이트 스킵
        if (meta.isEmpty()) {
            return new VideoMetaRefreshResponseDTO(false, toVideoResponse(video));
        }

        boolean updated = isYoutubeMetaChanged(video, meta);
        video.updateYoutubeMeta(meta.channelTitle(), meta.durationSec(), meta.thumbnailUrl());

        return new VideoMetaRefreshResponseDTO(updated, toVideoResponse(video));
    }

    // =========================================================
    // 권한/검증 헬퍼
    // =========================================================

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }

    private User requireProfessorUser(Long userId) {
        User u = findUserOrThrow(userId);
        RoleGuard.requireProfessor(u);
        return u;
    }

    private User requireAdminUser(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("관리자 사용자를 찾을 수 없습니다."));
        RoleGuard.requireAdmin(u);
        return u;
    }

    private void requireProfessorOrAdmin(UserRole role) {
        if (role != UserRole.PROFESSOR && role != UserRole.ADMIN) {
            throw new ForbiddenException("교수 또는 관리자만 조회할 수 있습니다.");
        }
    }

    private Lecture findLectureOrThrow(Long lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("강의를 찾을 수 없습니다."));
    }

    private void requireLectureOwner(Lecture lecture, Long currentUserId) {
        if (lecture.getProfessor() == null || lecture.getProfessor().getUserId() == null) {
            throw new ForbiddenException("강의 교수 정보가 없어 권한을 확인할 수 없습니다.");
        }
        if (!lecture.getProfessor().getUserId().equals(currentUserId)) {
            throw new ForbiddenException("본인 강의만 수정/등록할 수 있습니다.");
        }
    }

    private void requireNoExistingVideo(Long lectureId) {
        if (lectureVideoRepository.existsByLecture_LectureId(lectureId)) {
            throw new BadRequestException("이미 영상이 등록된 강의입니다. (영상 1개 정책)");
        }
    }

    private void requireLoggedInForEnrollingFilter(Long currentUserId) {
        if (currentUserId == null) {
            throw new BadRequestException("enrolling 필터는 로그인 후 사용 가능합니다.");
        }
    }

    private void requireYoutubeVideo(LectureVideo video) {
        if (video.getSourceType() != VideoSourceType.YOUTUBE) {
            throw new BadRequestException("유튜브 영상만 메타 갱신이 가능합니다.");
        }
    }

    private String extractYoutubeVideoIdOrThrow(String urlOrId) {
        String videoId = YoutubeParser.extractVideoId(urlOrId);
        if (videoId == null || videoId.isBlank()) {
            throw new BadRequestException("유효한 유튜브 URL 또는 videoId가 아닙니다.");
        }
        return videoId;
    }

    private String requireNonBlank(String s, String message) {
        if (s == null || s.isBlank()) throw new BadRequestException(message);
        return s;
    }

    private boolean isYoutubeMetaChanged(LectureVideo video, YoutubeClient.YoutubeMeta meta) {
        boolean channelChanged = meta.channelTitle() != null && !meta.channelTitle().isBlank()
                && (video.getYoutubeChannelTitle() == null || !meta.channelTitle().equals(video.getYoutubeChannelTitle()));

        boolean durationChanged = meta.durationSec() > 0 && meta.durationSec() != video.getDurationSec();

        boolean thumbChanged = meta.thumbnailUrl() != null && !meta.thumbnailUrl().isBlank()
                && (video.getThumbnailUrl() == null || !meta.thumbnailUrl().equals(video.getThumbnailUrl()));

        return channelChanged || durationChanged || thumbChanged;
    }

    // =========================================================
    // 파싱/정규화 & DTO 변환
    // =========================================================

    private LectureStatus parseLectureStatus(String status, String whenInvalidMessage) {
        if (status == null) throw new BadRequestException(whenInvalidMessage);
        try {
            return LectureStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException(whenInvalidMessage);
        }
    }

    private boolean isAll(String s) {
        return s != null && "ALL".equalsIgnoreCase(s.trim());
    }

    private String normalizeLanguage(String language) {
        if (language == null) return null;
        String s = language.trim();
        if (s.isEmpty() || "ALL".equalsIgnoreCase(s)) return null;
        return s;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String s = keyword.trim();
        if (s.isEmpty()) return null;
        return s;
    }

    private LectureResponseDTO toLectureResponse(Lecture l) {
        return new LectureResponseDTO(
                l.getLectureId(),
                l.getTitle(),
                l.getDescription(),
                l.getCountry(),
                l.getLanguage(),
                l.getProfessor().getUserId(),
                l.getProfessor().getUserNickname(),
                l.getStatus(),
                l.getApprovedBy() == null ? null : l.getApprovedBy().getUserId(),
                l.getApprovedAt(),
                l.getRejectReason(),
                l.getCreatedAt()
        );
    }

    private LectureVideoResponseDTO toVideoResponse(LectureVideo v) {
        return new LectureVideoResponseDTO(
                v.getVideoId(),
                v.getSourceType(),
                v.getDurationSec(),
                v.getThumbnailUrl(),
                v.getLocalPath(),
                v.getOriginalFilename(),
                v.getStoredFilename(),
                v.getMimeType(),
                v.getFileSizeBytes(),
                v.getYoutubeVideoId(),
                v.getYoutubeUrl(),
                v.getYoutubeChannelTitle()
        );
    }
}
