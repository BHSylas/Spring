package com.example.spring.security;

import org.springframework.security.core.Authentication;

public class CurrentUser {

    private CurrentUser() {}

    public static Long getUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Long l) return l;

        // 혹시 subject가 String으로 들어오게 바뀌는 경우 대비
        if (principal instanceof String s) return Long.valueOf(s);

        throw new IllegalStateException("지원하지 않는 principal 타입: " + principal.getClass());
    }
}
