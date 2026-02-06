package com.example.spring.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminRejectRequestDTO {

    @NotBlank(message = "반려 사유는 필수입니다.")
    private String reason;
}
