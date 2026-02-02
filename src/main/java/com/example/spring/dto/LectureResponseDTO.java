package com.example.spring.dto;

import com.example.spring.entity.LectureStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class LectureResponseDTO {
    private Long lectureId;
    private String title;
    private String description;
    private String country;
    private String language;

    private Long professorId;
    private String professorNickname;

    private LectureStatus status;

    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectReason;

    private LocalDateTime createdAt;
}
