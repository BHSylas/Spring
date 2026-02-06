package com.example.spring.controller;

import com.example.spring.dto.*;
import com.example.spring.entity.EnrollmentStatus;
import com.example.spring.security.CurrentUser;
import com.example.spring.service.EnrollmentService;
import com.example.spring.service.LectureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /**
     * 교수: 내 강의 목록
     * - status=ALL이면 전체
     * - 기본 정렬: 최신(lectureId desc)
     */
    @GetMapping
    public Page<LectureResponseDTO> listMyLectures(
            Authentication authentication,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = CurrentUser.getUserId(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lectureId"));
        return lectureService.listMyLectures(userId, status, pageable);
    }

    @PostMapping
    public LectureResponseDTO create(Authentication authentication, @RequestBody @Valid LectureCreateRequestDTO req) {
        Long userId = CurrentUser.getUserId(authentication);
        return lectureService.createLecture(userId, req);
    }

    @PatchMapping("/{lectureId}")
    public LectureResponseDTO update(Authentication authentication, @PathVariable Long lectureId,
                                  @RequestBody @Valid LectureUpdateRequestDTO req) {
        Long userId = CurrentUser.getUserId(authentication);
        return lectureService.updateLecture(userId, lectureId, req);
    }

    @PostMapping(value = "/{lectureId}/video/upload", consumes = "multipart/form-data")
    public LectureVideoResponseDTO uploadVideo(Authentication authentication,
                                            @PathVariable Long lectureId,
                                            @RequestPart("file") MultipartFile file) {
        Long userId = CurrentUser.getUserId(authentication);
        return lectureService.uploadLectureVideo(userId, lectureId, file);
    }

    @PostMapping("/{lectureId}/video/youtube")
    public LectureVideoResponseDTO attachYoutube(Authentication authentication,
                                              @PathVariable Long lectureId,
                                              @RequestBody @Valid YoutubeAttachRequestDTO req) {
        Long userId = CurrentUser.getUserId(authentication);
        return lectureService.attachYoutube(userId, lectureId, req);
    }

    @PostMapping("/{lectureId}/video/youtube/refresh-meta")
    public VideoMetaRefreshResponseDTO refreshYoutubeMeta(Authentication authentication,
                                                       @PathVariable Long lectureId) {
        Long userId = CurrentUser.getUserId(authentication);
        return lectureService.refreshYoutubeMeta(userId, lectureId);
    }

    @GetMapping("/{lectureId}/enrollments")
    public Page<LectureStudentEnrollmentItemDTO> listStudents(
            Authentication authentication,
            @PathVariable Long lectureId,
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = CurrentUser.getUserId(authentication);
        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Direction.DESC, "progressRate")
                        .and(Sort.by(Sort.Direction.DESC, "enrollmentId"))
        );
        return enrollmentService.listLectureStudents(userId, lectureId, status, pageable);
    }

    @GetMapping("/{lectureId}/stats")
    public InstructorLectureStatsDTO stats(Authentication authentication,
                                           @PathVariable Long lectureId) {
        Long userId = CurrentUser.getUserId(authentication);
        return lectureService.getLectureStats(userId, lectureId);
    }
}
