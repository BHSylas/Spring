package com.example.spring.controller;

import com.example.spring.dto.*;
import com.example.spring.security.CurrentUser;
import com.example.spring.service.LectureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/lectures")
public class AdminLectureController {

    private final LectureService lectureService;

    // =========================================================
    // 목록 - 체크리스트 요약 포함(상대적으로 무거움)
    // =========================================================

    /**
     * 관리자 강의 목록 (체크리스트 요약 포함)
     * - status=PENDING 기본
     */
    @GetMapping
    public Page<AdminLectureListItemDTO> listWithChecklist(
            Authentication authentication,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long adminId = adminId(authentication);
        return lectureService.adminListLecturesWithChecklist(adminId, status, pageable(page, size));
    }

    /**
     * PENDING인데 영상 없는 강의 목록 (체크리스트 요약 포함)
     */
    @GetMapping("/pending-without-video")
    public Page<AdminLectureListItemDTO> pendingWithoutVideoWithChecklist(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long adminId = adminId(authentication);
        return lectureService.adminListPendingWithoutVideo(adminId, pageable(page, size));
    }

    // =========================================================
    // 목록 - 카드용(썸네일 포함, 빠름)
    // =========================================================

    /**
     * 관리자 강의 카드 목록 (썸네일 포함)
     * - status=ALL이면 전체
     */
    @GetMapping("/cards")
    public Page<LectureListItemDTO> cards(
            Authentication authentication,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long adminId = adminId(authentication);
        return lectureService.adminLectureCardItems(adminId, status, pageable(page, size));
    }

    /**
     * PENDING인데 영상 없는 강의 카드 목록 (썸네일 포함 / 영상은 없으니 null로 내려감)
     */
    @GetMapping("/pending-without-video/cards")
    public Page<LectureListItemDTO> pendingWithoutVideoCards(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long adminId = adminId(authentication);
        return lectureService.adminPendingWithoutVideoCardItems(adminId, pageable(page, size));
    }

    // =========================================================
    // 대시보드/체크리스트 상세
    // =========================================================

    @GetMapping("/dashboard")
    public AdminLectureDashboardDTO dashboard(Authentication authentication) {
        Long adminId = adminId(authentication);
        return lectureService.getAdminLectureDashboard(adminId);
    }

    @GetMapping("/{lectureId}/checklist")
    public AdminLectureApprovalChecklistDTO checklist(Authentication authentication, @PathVariable Long lectureId) {
        Long adminId = adminId(authentication);
        return lectureService.getApprovalChecklist(adminId, lectureId);
    }

    // =========================================================
    // 승인/반려/비활성화
    // =========================================================

    @PatchMapping("/{lectureId}/approval")
    public LectureResponseDTO approve(
            Authentication authentication,
            @PathVariable Long lectureId,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        Long adminId = adminId(authentication);
        return lectureService.approveLecture(adminId, lectureId, force);
    }

    @PatchMapping("/{lectureId}/rejection")
    public LectureResponseDTO reject(
            Authentication authentication,
            @PathVariable Long lectureId,
            @RequestBody @Valid AdminRejectRequestDTO req
    ) {
        Long adminId = adminId(authentication);
        return lectureService.rejectLecture(adminId, lectureId, req.getReason());
    }

    @PatchMapping("/{lectureId}/inactivate")
    public LectureResponseDTO inactivate(
            Authentication authentication,
            @PathVariable Long lectureId
    ) {
        Long adminId = adminId(authentication);
        return lectureService.adminInactivateLecture(adminId, lectureId);
    }

    @PatchMapping("/{lectureId}/reactivate")
    public LectureResponseDTO reactivate(
            Authentication authentication,
            @PathVariable Long lectureId
    ) {
        Long adminId = adminId(authentication);
        return lectureService.adminReactivateLecture(adminId, lectureId);
    }

    // =========================================================
    // 공통 헬퍼
    // =========================================================

    private Long adminId(Authentication authentication) {
        return CurrentUser.getUserId(authentication);
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lectureId"));
    }
}