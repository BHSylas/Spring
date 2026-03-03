package com.example.spring.controller;

import com.example.spring.dto.*;
import com.example.spring.security.CurrentUser;
import com.example.spring.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Page<AdminUserDTO> list(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long adminId = CurrentUser.getUserId(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "userId"));
        return adminUserService.listUsers(adminId, keyword, pageable);
    }

    @GetMapping("/dashboard")
    public AdminUserDashboardDTO dashboard(Authentication authentication) {
        Long adminId = CurrentUser.getUserId(authentication);
        return adminUserService.getDashboard(adminId);
    }

    @GetMapping("/{userId}")
    public AdminUserDTO detail(Authentication authentication, @PathVariable Long userId) {
        Long adminId = CurrentUser.getUserId(authentication);
        return adminUserService.getUserDetail(adminId, userId);
    }

    @PatchMapping("/{userId}/role")
    public AdminUserDTO changeRole(
            Authentication authentication,
            @PathVariable Long userId,
            @RequestBody @Valid AdminUserRoleUpdateRequestDTO req
    ) {
        Long adminId = CurrentUser.getUserId(authentication);
        return adminUserService.changeUserRole(adminId, userId, req.role());
    }

    @PatchMapping("/{userId}/status")
    public AdminUserDTO changeStatus(
            Authentication authentication,
            @PathVariable Long userId,
            @RequestBody @Valid AdminUserStatusUpdateRequestDTO req
    ) {
        Long adminId = CurrentUser.getUserId(authentication);
        return adminUserService.changeUserStatus(adminId, userId, req.status());
    }

    @PostMapping("/{userId}/force-logout")
    public void forceLogout(Authentication authentication, @PathVariable Long userId) {
        Long adminId = CurrentUser.getUserId(authentication);
        adminUserService.forceLogout(adminId, userId);
    }
}