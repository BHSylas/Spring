package com.example.spring.dto;

import java.time.LocalDateTime;

/**
 * 관리자 대시보드(강의)용 카드 데이터
 */
public record AdminLectureDashboardDTO(
        long totalLectures,
        long pendingLectures,
        long approvedLectures,
        long rejectedLectures,
        long todayNewLectures,
        long todayApprovedLectures,
        long pendingWithoutVideo,
        LocalDateTime generatedAt
) {
}
