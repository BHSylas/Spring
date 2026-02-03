package com.example.spring.dto;

import com.example.spring.entity.Country;
import com.example.spring.entity.Level;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NpcStatsDTO {
    private Country country;
    private Level level;

    private long totalNpcCount;
    private long solvedCount;
    private long correctCount;

    private double officialAccuracy;
    private double accuracy;
}
