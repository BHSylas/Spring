package com.example.spring.controller;

import com.example.spring.common.exception.NotFoundException;
import com.example.spring.dto.*;
import com.example.spring.entity.EnrollmentStatus;
import com.example.spring.security.CurrentUser;
import com.example.spring.service.EnrollmentService;
import com.example.spring.service.LectureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@PreAuthorize("hasAnyRole('PROFESSOR','ADMIN')") // 교수/관리자만
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/instructor/lectures")
public class InstructorLectureController {

    private final LectureService lectureService;
    private final EnrollmentService enrollmentService;

    // =========================================================
    // 목록
    // =========================================================

    /**
     * 교수: 내 강의 목록 (기존 상세 DTO 유지)
     */
    @GetMapping
    public Page<LectureResponseDTO> listMyLectures(
            Authentication authentication,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = userId(authentication);
        return lectureService.listMyLectures(userId, status, pageable(page, size));
    }

    /**
     * 교수: 내 강의 카드 목록 (썸네일 포함)
     */
    @GetMapping("/cards")
    public Page<LectureListItemDTO> listMyLectureCards(
            Authentication authentication,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = userId(authentication);
        return lectureService.listMyLectureCardItems(userId, status, pageable(page, size));
    }

    // =========================================================
    // 강의 CRUD
    // =========================================================

    @PostMapping
    public LectureResponseDTO create(
            Authentication authentication,
            @RequestBody @Valid LectureCreateRequestDTO req
    ) {
        Long userId = userId(authentication);
        return lectureService.createLecture(userId, req);
    }

    @PatchMapping("/{lectureId}")
    public LectureResponseDTO update(
            Authentication authentication,
            @PathVariable Long lectureId,
            @RequestBody @Valid LectureUpdateRequestDTO req
    ) {
        Long userId = userId(authentication);
        return lectureService.updateLecture(userId, lectureId, req);
    }

    /**
     * 강의 삭제 (정책: 취소 제외 활성 수강생이 있으면 삭제 불가)
     */
    @DeleteMapping("/{lectureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLecture(Authentication authentication, @PathVariable Long lectureId) {
        Long userId = userId(authentication);
        lectureService.deleteLecture(userId, lectureId);
    }

    // =========================================================
    // 영상: 업로드/유튜브/삭제/교체/메타 갱신
    // =========================================================

    @PostMapping(value = "/{lectureId}/video/upload", consumes = "multipart/form-data")
    public LectureVideoResponseDTO uploadVideo(
            Authentication authentication,
            @PathVariable Long lectureId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        Long userId = CurrentUser.getUserId(authentication);
        return lectureService.uploadLectureVideo(userId, lectureId, file, thumbnail);
    }

    @PostMapping("/{lectureId}/video/youtube")
    public LectureVideoResponseDTO attachYoutube(
            Authentication authentication,
            @PathVariable Long lectureId,
            @RequestBody @Valid YoutubeAttachRequestDTO req
    ) {
        Long userId = userId(authentication);
        return lectureService.attachYoutube(userId, lectureId, req);
    }

    @DeleteMapping("/{lectureId}/video")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVideo(Authentication authentication, @PathVariable Long lectureId) {
        Long userId = userId(authentication);
        lectureService.deleteLectureVideo(userId, lectureId);
    }

    @PutMapping(value = "/{lectureId}/video/upload", consumes = "multipart/form-data")
    public LectureVideoResponseDTO replaceUpload(
            Authentication authentication,
            @PathVariable Long lectureId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        Long userId = CurrentUser.getUserId(authentication);
        return lectureService.replaceLectureVideoWithUpload(userId, lectureId, file, thumbnail);
    }

    @PutMapping("/{lectureId}/video/youtube")
    public LectureVideoResponseDTO replaceYoutube(
            Authentication authentication,
            @PathVariable Long lectureId,
            @RequestBody @Valid YoutubeAttachRequestDTO req
    ) {
        Long userId = userId(authentication);
        return lectureService.replaceLectureVideoWithYoutube(userId, lectureId, req);
    }

    @PostMapping("/{lectureId}/video/youtube/refresh-meta")
    public VideoMetaRefreshResponseDTO refreshYoutubeMeta(
            Authentication authentication,
            @PathVariable Long lectureId
    ) {
        Long userId = userId(authentication);
        return lectureService.refreshYoutubeMeta(userId, lectureId);
    }

    /**
     * 교수용 영상 메타 조회
     */
    @GetMapping("/{lectureId}/video")
    public LectureVideoResponseDTO getVideo(
            Authentication authentication,
            @PathVariable Long lectureId
    ) {
        userId(authentication); // 인증 확인용 (권한은 service에서 체크 가능)
        return lectureService.getLectureVideo(lectureId)
                .orElseThrow(() -> new NotFoundException("강의에 등록된 영상이 없습니다."));
    }

    // =========================================================
    // 수강생 목록 / 통계
    // =========================================================

    @GetMapping("/{lectureId}/enrollments")
    public Page<LectureStudentEnrollmentItemDTO> listStudents(
            Authentication authentication,
            @PathVariable Long lectureId,
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = userId(authentication);

        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Direction.DESC, "progressRate")
                        .and(Sort.by(Sort.Direction.DESC, "enrollmentId"))
        );

        return enrollmentService.listLectureStudents(userId, lectureId, status, pageable);
    }

    @GetMapping("/{lectureId}/stats")
    public InstructorLectureStatsDTO stats(Authentication authentication, @PathVariable Long lectureId) {
        Long userId = userId(authentication);
        return lectureService.getLectureStats(userId, lectureId);
    }

    // =========================================================
    // 공통 헬퍼
    // =========================================================

    private Long userId(Authentication authentication) {
        return CurrentUser.getUserId(authentication);
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lectureId"));
    }
}