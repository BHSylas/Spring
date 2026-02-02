package com.example.spring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LectureCreateRequestDTO {

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    @Size(max = 60)
    private String country;

    @NotBlank
    @Size(max = 60)
    private String language;
}
