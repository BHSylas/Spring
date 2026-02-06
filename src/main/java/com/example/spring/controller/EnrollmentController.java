package com.example.spring.controller;

import com.example.spring.dto.*;
import com.example.spring.security.CurrentUser;
import com.example.spring.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasAnyRole('USER','ADMIN')") // 학생/관리자만
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // 수강 신청(등록)
    @PostMapping("/{lectureId}")
    public EnrollmentResponseDTO enroll(Authentication authentication, @PathVariable Long lectureId) {
        Long userId = CurrentUser.getUserId(authentication);
        return enrollmentService.enroll(userId, lectureId);
    }

    // 내 수강 목록
    @GetMapping
    public Page<MyEnrollmentItemDTO> listMyEnrollments(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "recent") String sort
    ) {
        Long userId = CurrentUser.getUserId(authentication);
        return enrollmentService.listMyEnrollments(userId, status, sort, page, size);
    }

    // 내 수강 정보 조회
    @GetMapping("/{lectureId}")
    public EnrollmentResponseDTO getMyEnrollment(Authentication authentication,
                                                 @PathVariable Long lectureId) {
        Long userId = CurrentUser.getUserId(authentication);
        return enrollmentService.getMyEnrollment(userId, lectureId);
    }

    // 진도 업데이트
    @PutMapping("/{lectureId}/progress")
    public EnrollmentResponseDTO updateProgress(Authentication authentication,
                                             @PathVariable Long lectureId,
                                             @RequestBody @Valid ProgressUpdateRequestDTO req) {
        Long userId = CurrentUser.getUserId(authentication);
        return enrollmentService.updateProgress(userId, lectureId, req);
    }

    // 수강 취소(status=CANCELED 처리)
    @DeleteMapping("/{lectureId}")
    public EnrollmentResponseDTO cancelEnrollment(Authentication authentication,
                                                  @PathVariable Long lectureId) {
        Long userId = CurrentUser.getUserId(authentication);
        return enrollmentService.cancelEnrollment(userId, lectureId);
    }
}
