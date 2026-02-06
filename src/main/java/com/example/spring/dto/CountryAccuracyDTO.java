package com.example.spring.dto;

import com.example.spring.entity.Country;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CountryAccuracyDTO {
    private Country country;
    private List<LevelAccuracyDTO> levels;
}
