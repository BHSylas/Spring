package com.example.spring.service;

import com.example.spring.dto.*;
import com.example.spring.entity.*;
import com.example.spring.common.exception.BadRequestException;
import com.example.spring.common.exception.ForbiddenException;
import com.example.spring.common.exception.NotFoundException;
import com.example.spring.repository.*;
import com.example.spring.security.RoleGuard;
import com.example.spring.storage.LocalFileStorage;
import com.example.spring.youtube.YoutubeClient;
import com.example.spring.youtube.YoutubeParser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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

    @Transactional
    public LectureResponseDTO createLecture(Long currentUserId, LectureCreateRequestDTO req) {
        User professor = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        RoleGuard.requireProfessor(professor);

        Lecture lecture = Lecture.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .country(req.getCountry())
                .language(req.getLanguage())
                .professor(professor)
                .build();

        Lecture saved = lectureRepository.save(lecture);
        return toLectureResponse(saved);
    }

    @Transactional
    public LectureResponseDTO updateLecture(Long currentUserId, Long lectureId, LectureUpdateRequestDTO req) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("강의를 찾을 수 없습니다."));

        if (!lecture.getProfessor().getUserId().equals(currentUserId)) {
            throw new ForbiddenException("본인 강의만 수정할 수 있습니다.");
        }

        lecture.updateInfo(req.getTitle(), req.getDescription(), req.getCountry(), req.getLanguage());
        return toLectureResponse(lecture);
    }

    /**
     * 개선된 강의 목록
     * - status=APPROVED만 기본 노출 (학생 기준 안전)
     * - language=ALL이면 언어 필터 없음
     * - enrolling=true/false면 '내 수강중' / '미수강' 필터
     * - total count 정확(DB 필터)
     */
    @Transactional
    public Page<LectureResponseDTO> listApprovedWithFilters(Long currentUserId, String language, Boolean enrolling, Pageable pageable) {
        boolean allLang = (language == null || language.isBlank() || "ALL".equalsIgnoreCase(language));

        // enrolling 필터 없으면 기존처럼 승인 강의만
        if (enrolling == null) {
            Page<Lecture> page = allLang
                    ? lectureRepository.findByStatus(LectureStatus.APPROVED, pageable)
                    : lectureRepository.findByStatusAndLanguage(LectureStatus.APPROVED, language, pageable);

            return page.map(this::toLectureResponse);
        }

        // enrolling=true/false면 내 수강 강의 id 목록으로 DB 필터
        List<Long> myLectureIds = enrollmentRepository.findLectureIdsByUserId(currentUserId);

        // enrolling=true인데 내가 수강중인 강의가 0개면 바로 빈 페이지
        if (enrolling && myLectureIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<Lecture> page;
        if (allLang) {
            page = enrolling
                    ? lectureRepository.findByStatusAndLectureIdIn(LectureStatus.APPROVED, myLectureIds, pageable)
                    : lectureRepository.findByStatusAndLectureIdNotIn(LectureStatus.APPROVED, myLectureIds, pageable);
        } else {
            page = enrolling
                    ? lectureRepository.findByStatusAndLanguageAndLectureIdIn(LectureStatus.APPROVED, language, myLectureIds, pageable)
                    : lectureRepository.findByStatusAndLanguageAndLectureIdNotIn(LectureStatus.APPROVED, language, myLectureIds, pageable);
        }

        return page.map(this::toLectureResponse);
    }

    // 기존 메서드도 유지(호환)
    @Transactional
    public Page<LectureResponseDTO> listApproved(Pageable pageable) {
        return lectureRepository.findByStatus(LectureStatus.APPROVED, pageable)
                .map(this::toLectureResponse);
    }

    @Transactional
    public LectureResponseDTO getLectureDetail(Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("강의를 찾을 수 없습니다."));
        return toLectureResponse(lecture);
    }

    @Transactional
    public LectureVideoResponseDTO uploadLectureVideo(Long currentUserId, Long lectureId, MultipartFile file) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("강의를 찾을 수 없습니다."));

        if (!lecture.getProfessor().getUserId().equals(currentUserId)) {
            throw new ForbiddenException("본인 강의에만 업로드할 수 있습니다.");
        }

        if (lectureVideoRepository.existsByLecture_LectureId(lectureId)) {
            throw new BadRequestException("이미 영상이 등록된 강의입니다. (영상 1개 정책)");
        }

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

        LectureVideo saved = lectureVideoRepository.save(video);
        return toVideoResponse(saved);
    }

    @Transactional
    public LectureVideoResponseDTO attachYoutube(Long currentUserId, Long lectureId, YoutubeAttachRequestDTO req) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("강의를 찾을 수 없습니다."));

        if (!lecture.getProfessor().getUserId().equals(currentUserId)) {
            throw new ForbiddenException("본인 강의에만 등록할 수 있습니다.");
        }

        if (lectureVideoRepository.existsByLecture_LectureId(lectureId)) {
            throw new BadRequestException("이미 영상이 등록된 강의입니다. (영상 1개 정책)");
        }

        String videoId = YoutubeParser.extractVideoId(req.getYoutubeUrlOrId());
        if (videoId == null || videoId.isBlank()) {
            throw new BadRequestException("유효한 유튜브 URL 또는 videoId가 아닙니다.");
        }

        YoutubeClient.YoutubeMeta meta = youtubeClient.fetchMetaSafe(videoId);
        String url = YoutubeParser.toWatchUrl(videoId);

        LectureVideo video = LectureVideo.ofYoutube(
                lecture,
                videoId,
                url,
                meta.channelTitle(),
                meta.durationSec(),
                meta.thumbnailUrl()
        );

        LectureVideo saved = lectureVideoRepository.save(video);
        return toVideoResponse(saved);
    }

    public Optional<LectureVideoResponseDTO> getLectureVideo(Long lectureId) {
        return lectureVideoRepository.findByLecture_LectureId(lectureId).map(this::toVideoResponse);
    }

    @Transactional
    public LectureResponseDTO approveLecture(Long adminUserId, Long lectureId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("관리자 사용자를 찾을 수 없습니다."));
        RoleGuard.requireAdmin(admin);

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("강의를 찾을 수 없습니다."));

        lecture.approve(admin);
        return toLectureResponse(lecture);
    }

    @Transactional
    public LectureResponseDTO rejectLecture(Long adminUserId, Long lectureId, String reason) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("관리자 사용자를 찾을 수 없습니다."));
        RoleGuard.requireAdmin(admin);

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("강의를 찾을 수 없습니다."));

        lecture.reject(admin, reason);
        return toLectureResponse(lecture);
    }

    @Transactional
    public VideoMetaRefreshResponseDTO refreshYoutubeMeta(Long currentUserId, Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("강의를 찾을 수 없습니다."));

        User caller = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        boolean isAdmin = UserRole.fromCode(caller.getUserRole()) == UserRole.ADMIN;

        if (!isAdmin && !lecture.getProfessor().getUserId().equals(currentUserId)) {
            throw new ForbiddenException("본인 강의만 메타 갱신할 수 있습니다.");
        }

        LectureVideo video = lectureVideoRepository.findByLecture_LectureId(lectureId)
                .orElseThrow(() -> new NotFoundException("강의에 등록된 영상이 없습니다."));

        if (video.getSourceType() != VideoSourceType.YOUTUBE) {
            throw new BadRequestException("유튜브 영상만 메타 갱신이 가능합니다.");
        }

        String videoId = video.getYoutubeVideoId();
        if (videoId == null || videoId.isBlank()) {
            throw new BadRequestException("youtubeVideoId가 비어있습니다.");
        }

        YoutubeClient.YoutubeMeta meta = youtubeClient.fetchMetaSafe(videoId);

        boolean updated =
                (meta.channelTitle() != null && !meta.channelTitle().isBlank()
                        && (video.getYoutubeChannelTitle() == null || !meta.channelTitle().equals(video.getYoutubeChannelTitle())))
                        || (meta.durationSec() > 0 && meta.durationSec() != video.getDurationSec())
                        || (meta.thumbnailUrl() != null && !meta.thumbnailUrl().isBlank()
                        && (video.getThumbnailUrl() == null || !meta.thumbnailUrl().equals(video.getThumbnailUrl())));

        video.updateYoutubeMeta(meta.channelTitle(), meta.durationSec(), meta.thumbnailUrl());

        return new VideoMetaRefreshResponseDTO(updated, toVideoResponse(video));
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
