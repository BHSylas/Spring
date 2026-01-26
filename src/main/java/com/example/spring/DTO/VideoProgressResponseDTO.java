package com.example.spring.DTO;

import com.example.spring.Entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class VideoProgressResponseDTO {
    private EnrollmentStatus status;
    private int progressRate;
}
