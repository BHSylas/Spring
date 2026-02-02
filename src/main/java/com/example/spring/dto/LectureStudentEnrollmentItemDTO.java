package com.example.spring.dto;

import java.time.LocalDateTime;

public record LectureStudentEnrollmentItemDTO(
        Long enrollmentId,
        Long studentId,
        String studentNickname,
        String studentEmail,
        String status,
        int progressRate,
        int lastWatchedTime,
        int totalDuration,
        LocalDateTime lastAccessedAt,
        LocalDateTime completedAt
) {}
