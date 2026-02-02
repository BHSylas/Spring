package com.example.spring.dto;

import java.time.LocalDateTime;

public record MyEnrollmentItemDTO(
        Long enrollmentId,
        // Lecture
        Long lectureId,
        String title,
        String country,
        String language,
        Long professorId,
        String professorNickname,
        // Progress
        String status,
        int progressRate,
        int lastWatchedTime,
        int totalDuration,
        LocalDateTime lastAccessedAt,
        LocalDateTime completedAt
) {}
