package com.example.spring.dto;

import com.example.spring.entity.Country;
import com.example.spring.entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public class LectureListResponseDTO {
    private Long lectureId;
    private String title;
    private String description;
    private String country;
    private String language;

}
