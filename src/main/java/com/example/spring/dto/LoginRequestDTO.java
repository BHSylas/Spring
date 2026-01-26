package com.example.spring.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequestDTO {
    @Email @NotBlank
    private String email;

    @NotBlank
    private String password;
}
