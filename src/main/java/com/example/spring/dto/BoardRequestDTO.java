package com.example.spring.dto;

import com.example.spring.entity.BoardType;
import lombok.Getter;

@Getter
public class BoardRequestDTO {
    private BoardType boardType;
    private Long lectureId;
    private String title;
    private String content;
    private boolean pinned;

}
