package com.example.spring.security;

import com.example.spring.entity.User;
import com.example.spring.entity.UserRole;
import com.example.spring.common.exception.ForbiddenException;

public class RoleGuard {

    private RoleGuard() {}

    public static void requireProfessor(User user) {
        UserRole role = UserRole.fromCode(user.getUserRole());
        if (!(role == UserRole.PROFESSOR || role == UserRole.ADMIN)) {
            throw new ForbiddenException("교수 권한이 필요합니다.");
        }
    }

    public static void requireUser(User user) {
        UserRole role = UserRole.fromCode(user.getUserRole());
        if (!(role == UserRole.USER || role == UserRole.ADMIN)) {
            throw new ForbiddenException("학생(USER) 권한이 필요합니다.");
        }
    }

    public static void requireAdmin(User user) {
        UserRole role = UserRole.fromCode(user.getUserRole());
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException("관리자 권한이 필요합니다.");
        }
    }
}
