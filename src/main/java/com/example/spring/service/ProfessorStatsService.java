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
@Transactional(readOnly = true)
public class ProfessorStatsService {
    private final NpcConversationRepository conversationRepository;
    private final UserNpcAnswerRepository userNpcAnswerRepository;

    public List<NpcStatsDTO> getProfessorStats(Long professorId) {
        return List.of(Country.values()).stream()
                .flatMap(country -> List.of(Level.values()).stream()
                        .map(level -> {
                            long totalNpc = conversationRepository.countByProfessor_UserIdAndCountryAndLevel(professorId, country, level);
                            long solved = userNpcAnswerRepository.countByNpcConversation_Professor_UserIdAndCountryAndLevelAndCorrectTrue(professorId, country, level);
                            long correct = userNpcAnswerRepository.countByNpcConversation_Professor_UserIdAndCountryAndLevelAndCorrectTrue(professorId, country, level);

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
                )
                .toList();

    }
}
