package com.example.spring.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardAnswerResponseDTO {
    private Long answerId;
    private Long writerId;
    private String writerName;
    private String content;
    private LocalDateTime createdAt;
}
