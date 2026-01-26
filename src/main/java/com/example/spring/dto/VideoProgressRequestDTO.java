package com.example.spring.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class VideoProgressRequestDTO {
    private long lectureId;
    private int currentTime;
    private int duration;
    private LocalDateTime lastAccessedAt;

}
