package com.example.spring.dto;

import com.example.spring.entity.VideoSourceType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LectureVideoResponseDTO {

    private Long videoId;
    private VideoSourceType sourceType;

    private int durationSec;
    private String thumbnailUrl;

    // upload
    private String localPath;
    private String originalFilename;
    private String storedFilename;
    private String mimeType;
    private Long fileSizeBytes;

    // youtube
    private String youtubeVideoId;
    private String youtubeUrl;
    private String youtubeChannelTitle;
}
