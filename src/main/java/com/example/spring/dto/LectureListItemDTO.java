package com.example.spring.dto;

import com.example.spring.entity.LectureStatus;
import com.example.spring.entity.VideoSourceType;

import java.time.LocalDateTime;

public record LectureListItemDTO(
        Long lectureId,
        String title,
        String country,
        String language,
        LectureStatus status,

        Long professorId,
        String professorNickname,

        // 대표 영상(강의당 1개 정책)
        VideoSourceType videoSourceType,
        String thumbnailUrl,
        Integer durationSec,
        String youtubeVideoTitle,
        String youtubeChannelTitle,

        LocalDateTime createdAt
) {}