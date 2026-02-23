package com.example.spring.dto;

import com.example.spring.entity.BoardType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyCommentResponseDTO {

    // 댓글 정보
    private Long commentId;
    private String content;
    private int depth;             // 0: 댓글, 1: 대댓글
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 댓글이 달린 게시글 정보 (어느 글에 단 댓글인지 맥락 제공)
    private Long boardId;
    private BoardType boardType;
    private String boardTitle;

}
