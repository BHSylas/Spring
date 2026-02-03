package com.example.spring.service;

import com.example.spring.dto.CountryAccuracyDTO;
import com.example.spring.dto.LevelAccuracyDTO;
import com.example.spring.dto.MyLearningsStatsResponseDTO;
import com.example.spring.entity.Country;
import com.example.spring.entity.Level;
import com.example.spring.repository.NpcConversationRepository;
import com.example.spring.repository.UserNpcAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyLearningStatsService {
    private final NpcConversationRepository conversationRepository;
    private final UserNpcAnswerRepository userNpcAnswerRepository;

    public MyLearningsStatsResponseDTO getMyLearningsStats(Long userId) {
        List<CountryAccuracyDTO> countryStats = List.of(Country.values()).stream()
                .map(country -> CountryAccuracyDTO.builder()
                        .country(country)
                        .levels(buildLevelStats(userId, country))
                        .build())
                .toList();

        return MyLearningsStatsResponseDTO.builder()
                .userId(userId)
                .stats(countryStats)
                .build();
    }

    private List<LevelAccuracyDTO> buildLevelStats(Long userId, Country country) {

        return List.of(Level.values()).stream()
                .map(level -> {
                    long totalProblems = conversationRepository.countByCountryAndLevel(country, level);
                    long solvedProblems = userNpcAnswerRepository.countByUserUserIdAndCountryAndLevel(userId, country, level);
                    long correctProblems = userNpcAnswerRepository.countByUserUserIdAndCountryAndLevelAndCorrectTrue(userId, country, level);

                    double officialAccuracy = totalProblems == 0 ? 0.0 :
                            (double) correctProblems / totalProblems * 100;

                    double userAccuracy = solvedProblems == 0 ? 0.0 :
                            (double) correctProblems / solvedProblems * 100;

                    return LevelAccuracyDTO.builder()
                            .level(level)
                            .totalProblems(totalProblems)
                            .solvedProblems(solvedProblems)
                            .correctProblems(correctProblems)
                            .officialAccuracy(officialAccuracy)
                            .userAccuracy(userAccuracy)
                            .build();
                })
                .toList();
    }
}
