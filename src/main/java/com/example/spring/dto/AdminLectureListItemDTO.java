package com.example.spring.dto;

import com.example.spring.entity.LectureStatus;

import java.time.LocalDateTime;

public record AdminLectureListItemDTO(
        Long lectureId,
        String title,
        String country,
        String language,
        LectureStatus status,
        Long professorId,
        String professorNickname,
        LocalDateTime createdAt,

        // 체크리스트 요약 (빨간불/초록불용)
        boolean checklistPassed,
        int checklistRequiredTotal,
        int checklistRequiredFailed
) {}
