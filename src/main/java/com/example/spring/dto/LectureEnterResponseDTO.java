package com.example.spring.dto;

import com.example.spring.entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LectureEnterResponseDTO {
    private Long lectureId;
    private String title;

    private EnrollmentStatus status;
    private int progressRate;
    private int lastWatchedTime;
    private int totalDuration;


}
