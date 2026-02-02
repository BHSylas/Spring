package com.example.spring.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProgressUpdateRequestDTO {

    @Min(0) @Max(100)
    private Integer progress;

    // 마지막 시청 위치(초)
    @NotNull
    @Min(0)
    private Integer lastWatchedTime;

    // 전체 길이(초)
    @NotNull
    @Min(1)
    private Integer totalDuration;
}
