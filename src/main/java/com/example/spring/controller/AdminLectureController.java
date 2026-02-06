package com.example.spring.controller;

import com.example.spring.dto.AdminRejectRequestDTO;
import com.example.spring.dto.LectureResponseDTO;
import com.example.spring.security.CurrentUser;
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

@PreAuthorize("hasRole('ADMIN')") // 관리자만
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/lectures")
public class AdminLectureController {

    private final LectureService lectureService;

    @GetMapping
    public Page<LectureResponseDTO> list(
            Authentication authentication,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long adminId = CurrentUser.getUserId(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lectureId"));
        return lectureService.adminListLectures(adminId, status, pageable);
    }

    /**
     * 승인 처리 (상태 변경) => PATCH
     */
    @PatchMapping("/{lectureId}/approval")
    public LectureResponseDTO approve(Authentication authentication, @PathVariable Long lectureId) {
        Long adminId = CurrentUser.getUserId(authentication);
        return lectureService.approveLecture(adminId, lectureId);
    }

    /**
     * 반려 처리 (상태 변경) => PATCH
     */
    @PatchMapping("/{lectureId}/rejection")
    public LectureResponseDTO reject(
            Authentication authentication,
            @PathVariable Long lectureId,
            @RequestBody @Valid AdminRejectRequestDTO req
    ) {
        Long adminId = CurrentUser.getUserId(authentication);
        return lectureService.rejectLecture(adminId, lectureId, req.getReason());
    }
}
