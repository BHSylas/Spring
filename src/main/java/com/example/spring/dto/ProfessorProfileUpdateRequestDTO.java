package com.example.spring.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProfessorProfileUpdateRequestDTO {

    @Size(max = 1000)
    private String bio;

    @Size(max = 200)
    private String specialty;

    @Size(max = 1000)
    private String career;

    @Size(max = 500)
    private String profileImageUrl;

    @Size(max = 200)
    private String office;

    @Email
    @Size(max = 120)
    private String contactEmail;
}