package com.example.spring.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateMyProfileRequestDTO {

    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String name;

    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
    private String nickname;
}