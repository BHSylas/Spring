package com.example.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class AnswerSubmitResponseDTO {
    private boolean correct;
    private boolean locked;
    private int attempts;
    private Long nextConversationId;
    private String explanation;
    private List<String> correctAnswer;

}
