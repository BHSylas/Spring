package com.example.spring.controller;

import com.example.spring.dto.AdminRejectRequestDTO;
import com.example.spring.dto.LectureResponseDTO;
import com.example.spring.security.CurrentUser;
import com.example.spring.service.LectureService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ADMIN')") // 관리자만
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/lectures")
public class AdminLectureController {

    private final LectureService lectureService;

    @PostMapping("/{lectureId}/approve")
    public LectureResponseDTO approve(Authentication authentication, @PathVariable Long lectureId) {
        Long adminId = CurrentUser.getUserId(authentication);
        return lectureService.approveLecture(adminId, lectureId);
    }

    @PostMapping("/{lectureId}/reject")
    public LectureResponseDTO reject(Authentication authentication, @PathVariable Long lectureId,
                                  @RequestBody AdminRejectRequestDTO req) {
        Long adminId = CurrentUser.getUserId(authentication);
        return lectureService.rejectLecture(adminId, lectureId, req.getReason());
    }
}
