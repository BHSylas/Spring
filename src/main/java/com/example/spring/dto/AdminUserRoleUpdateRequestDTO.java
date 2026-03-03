package com.example.spring.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserRoleUpdateRequestDTO(
        @NotBlank(message = "role 값은 필수입니다.")
        String role
) {
}