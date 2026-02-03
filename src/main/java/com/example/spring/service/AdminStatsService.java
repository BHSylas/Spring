package com.example.spring.service;

import com.example.spring.dto.NpcStatsDTO;
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
@Transactional(readOnly=true)
public class AdminStatsService {
    private final NpcConversationRepository npcConversationRepository;
    private final UserNpcAnswerRepository userNpcAnswerRepository;

    public List<NpcStatsDTO> getNpcStats() {
        return List.of(Country.values()).stream()
                .flatMap(country -> List.of(Level.values()).stream()
                        .map(level -> {
                            long totalNpc = npcConversationRepository.countByCountryAndLevel(country, level);
                            long solved = userNpcAnswerRepository.countByCountryAndLevel(country, level);
                            long correct = userNpcAnswerRepository.countByCountryAndLevelAndCorrectTrue(country, level);

                            double accuracy = solved == 0 ? 0.0:
                                    (double) correct / solved * 100;

                            double officialAccuracy = totalNpc == 0 ? 0.0:
                                    (double) correct / totalNpc * 100;

                            return NpcStatsDTO.builder()
                                    .country(country)
                                    .level(level)
                                    .totalNpcCount(totalNpc)
                                    .solvedCount(solved)
                                    .correctCount(correct)
                                    .accuracy(accuracy)
                                    .officialAccuracy(officialAccuracy)
                                    .build();
                        })

                ).toList();


    }
}
