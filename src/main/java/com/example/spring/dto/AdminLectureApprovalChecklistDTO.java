package com.example.spring.dto;

import java.util.List;

/**
 * 관리자 승인 체크리스트 결과
 */
public record AdminLectureApprovalChecklistDTO(
        Long lectureId,
        boolean canApprove,
        List<Item> items
) {
    public record Item(
            String key,
            String title,
            boolean required,
            boolean passed,
            String detail
    ) {}
}
