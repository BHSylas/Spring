package com.example.spring.dto;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
        String message,
        int status,
        String path,
        LocalDateTime timestamp
) {}
