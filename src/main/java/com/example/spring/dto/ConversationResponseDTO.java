package com.example.spring.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConversationResponseDTO {
    private boolean correct;
    private Long nextConversationId;
    private String explanation;
}
