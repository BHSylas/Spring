package com.example.spring.service;

import com.example.spring.common.exception.BadRequestException;
import com.example.spring.common.exception.NotFoundException;
import com.example.spring.dto.*;
import com.example.spring.entity.User;
import com.example.spring.entity.UserRole;
import com.example.spring.entity.UserStatus;
import com.example.spring.repository.RefreshTokenRepository;
import com.example.spring.repository.UserRepository;
import com.example.spring.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public Page<AdminUserDTO> listUsers(Long adminUserId, String keyword, Pageable pageable) {
        requireAdmin(adminUserId);

        String k = normalizeKeyword(keyword);

        Page<User> page = (k == null)
                ? userRepository.findAll(pageable)
                : userRepository.findByUserEmailContainingIgnoreCaseOrUserNicknameContainingIgnoreCase(k, k, pageable);

        return page.map(this::toUserDto);
    }

    public AdminUserDTO getUserDetail(Long adminUserId, Long targetUserId) {
        requireAdmin(adminUserId);
        User user = findUserOrThrow(targetUserId);
        return toUserDto(user);
    }

    @Transactional
    public AdminUserDTO changeUserRole(Long adminUserId, Long targetUserId, String roleText) {
        User admin = requireAdmin(adminUserId);
        User target = findUserOrThrow(targetUserId);

        UserRole newRole = parseRole(roleText);

        // 자기 자신을 USER로 낮추는 것 방지
        if (admin.getUserId().equals(target.getUserId()) && newRole == UserRole.USER) {
            throw new BadRequestException("본인 계정을 일반 사용자로 변경할 수 없습니다.");
        }

        target.changeRole(newRole.getCode());

        // 역할 변경 후 재로그인 유도
        refreshTokenRepository.revokeAllActiveByUserId(targetUserId);

        return toUserDto(target);
    }

    @Transactional
    public AdminUserDTO changeUserStatus(Long adminUserId, Long targetUserId, String statusText) {
        User admin = requireAdmin(adminUserId);
        User target = findUserOrThrow(targetUserId);

        UserStatus newStatus = parseStatus(statusText);

        // 자기 자신 BLOCKED 방지
        if (admin.getUserId().equals(target.getUserId()) && newStatus == UserStatus.BLOCKED) {
            throw new BadRequestException("본인 계정을 차단할 수 없습니다.");
        }

        target.changeStatus(newStatus);

        // 상태 변경 시 세션 정리
        refreshTokenRepository.revokeAllActiveByUserId(targetUserId);

        return toUserDto(target);
    }

    @Transactional
    public void forceLogout(Long adminUserId, Long targetUserId) {
        requireAdmin(adminUserId);
        findUserOrThrow(targetUserId);
        refreshTokenRepository.revokeAllActiveByUserId(targetUserId);
    }

    public AdminUserDashboardDTO getDashboard(Long adminUserId) {
        requireAdmin(adminUserId);

        long total = userRepository.count();
        long normal = userRepository.countByUserRole(UserRole.USER.getCode());
        long professors = userRepository.countByUserRole(UserRole.PROFESSOR.getCode());
        long admins = userRepository.countByUserRole(UserRole.ADMIN.getCode());

        long activeUsers = userRepository.countByUserStatus(UserStatus.ACTIVE);
        long blockedUsers = userRepository.countByUserStatus(UserStatus.BLOCKED);

        long todaySignups = userRepository.countByCreatedAtGreaterThanEqual(LocalDate.now().atStartOfDay());

        return new AdminUserDashboardDTO(
                total,
                normal,
                professors,
                admins,
                activeUsers,
                blockedUsers,
                todaySignups,
                LocalDateTime.now()
        );
    }

    // =========================
    // 내부 헬퍼
    // =========================

    private User requireAdmin(Long userId) {
        User admin = findUserOrThrow(userId);
        RoleGuard.requireAdmin(admin);
        return admin;
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String s = keyword.trim();
        return s.isEmpty() ? null : s;
    }

    private UserRole parseRole(String roleText) {
        if (roleText == null) {
            throw new BadRequestException("role 값이 필요합니다. (USER, PROFESSOR, ADMIN)");
        }

        try {
            return UserRole.valueOf(roleText.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("role 값이 올바르지 않습니다. (USER, PROFESSOR, ADMIN)");
        }
    }

    private UserStatus parseStatus(String statusText) {
        if (statusText == null) {
            throw new BadRequestException("status 값이 필요합니다. (ACTIVE, BLOCKED)");
        }

        try {
            return UserStatus.valueOf(statusText.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("status 값이 올바르지 않습니다. (ACTIVE, BLOCKED)");
        }
    }

    private AdminUserDTO toUserDto(User user) {
        UserRole role = UserRole.fromCode(user.getUserRole());
        return new AdminUserDTO(
                user.getUserId(),
                user.getUserEmail(),
                user.getUserName(),
                user.getUserNickname(),
                user.getUserRole(),
                role.name(),
                user.getUserStatus().name(),
                user.getCreatedAt()
        );
    }
}