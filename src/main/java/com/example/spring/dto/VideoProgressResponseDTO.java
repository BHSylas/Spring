package com.example.spring.dto;

import com.example.spring.entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class VideoProgressResponseDTO {
    private EnrollmentStatus status;
    private int progressRate;
}
