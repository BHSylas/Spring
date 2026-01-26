package com.example.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenPairDTO {
    private String accessToken;
    private String refreshToken;
    private String userNickname;
    private String userName;
}
