package com.example.spring.dto;

public record InstructorLectureStatsDTO(
        Long lectureId,
        long enrolledCount,
        long completedCount,
        double avgProgressRate
) {}
