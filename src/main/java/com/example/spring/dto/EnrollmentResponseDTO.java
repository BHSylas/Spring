package com.example.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EnrollmentResponseDTO {
    private Long enrollmentId;
    private Long lectureId;

    private int progressRate;
    private int lastWatchedTime;
    private int totalDuration;

    private String status;
}
