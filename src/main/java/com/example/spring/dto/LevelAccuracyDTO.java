package com.example.spring.dto;

import com.example.spring.entity.Level;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LevelAccuracyDTO {
    private Level level;
    //공식 지표
    private long totalProblems;
    private long correctProblems;
    private double officialAccuracy;
    //사용자 지표
    private long solvedProblems;
    private double userAccuracy;

}
