package com.example.spring.dto;

import com.example.spring.entity.BoardType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder

public class BoardResponseDTO {
    private Long boardId;
    private BoardType boardType;
    private Long lectureId;
    private String title;
    private String content;
    private String writerName;
    private Boolean answered;
    private Boolean pinned;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
