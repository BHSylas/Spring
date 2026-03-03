package com.example.spring.dto;

import java.time.LocalDateTime;

public record AdminUserDTO(
        Long userId,
        String email,
        String name,
        String nickname,
        byte roleCode,
        String roleName,
        String status,
        LocalDateTime createdAt
) {
}