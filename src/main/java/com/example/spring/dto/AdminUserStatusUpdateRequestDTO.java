package com.example.spring.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserStatusUpdateRequestDTO(
        @NotBlank(message = "status 값은 필수입니다.")
        String status
) {
}