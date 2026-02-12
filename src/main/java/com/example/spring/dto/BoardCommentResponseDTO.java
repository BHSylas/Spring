package com.example.spring.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter

public class BoardCommentResponseDTO {
    private Long commentId;
    private Long writerId;
    private String writerName;
    private String content;
    private int depth;
    private LocalDateTime createdAt;

    private List<BoardCommentResponseDTO> children;
}
