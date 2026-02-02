package com.example.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VideoMetaRefreshResponseDTO {
    private boolean updated;              // 이번 호출로 실제로 DB 값이 바뀌었는지
    private LectureVideoResponseDTO video;   // 갱신 후(또는 그대로인) 영상 정보
}
