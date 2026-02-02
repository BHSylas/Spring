package com.example.spring.service;

import com.example.spring.dto.AnswerSubmitRequestDTO;
import com.example.spring.dto.AnswerSubmitResponseDTO;
import com.example.spring.entity.Level;
import com.example.spring.entity.NPCConversation;
import com.example.spring.entity.User;
import com.example.spring.entity.UserNpcAnswer;
import com.example.spring.repository.NpcConversationRepository;
import com.example.spring.repository.UserNpcAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AnswerSubmitService {

    private final NpcConversationRepository npcConversationRepository;
    private final UserNpcAnswerRepository userNpcAnswerRepository;

    public AnswerSubmitResponseDTO answerSubmit(User user, Long npcConversationId, List<String> userAnswer) {
        NPCConversation npc = npcConversationRepository.findById(npcConversationId).orElseThrow(()
        -> new IllegalArgumentException("NPC 대화 없음"));

        UserNpcAnswer record = userNpcAnswerRepository
                .findByUserAndNpcConversation(user, npc)
                .orElseGet(() -> UserNpcAnswer.builder()
                        .user(user)
                        .npcConversation(npc)
                        .country(npc.getCountry())
                        .level(npc.getLevel())
                        .place(npc.getPlace())
                        .attempts(0)
                        .correct(false)
                        .locked(false)
                        .build());

        // 이미 종료된 문제
        if (record.isLocked()) {
            return AnswerSubmitResponseDTO.builder()
                    .correct(record.isCorrect())
                    .locked(true)
                    .attempts(record.getAttempts())
                    .correctAnswer(npc.getAnswers())
                    .explanation(npc.getExplanation())
                    .nextConversationId(npc.getNextConversationId())
                    .build();
        }


        int attempts = record.getAttempts() + 1;
        boolean correct = checkAnswer(npc, userAnswer);

        record.setAttempts(attempts);
        record.setAnsweredAt(LocalDateTime.now());

        // 정답
        if (correct) {
            record.setCorrect(true);
            record.setLocked(true);
        }

        if (!correct && attempts >= 3) {
            record.setLocked(true);
        }

        userNpcAnswerRepository.save(record);

        return AnswerSubmitResponseDTO.builder()
                .correct(correct)
                .attempts(attempts)
                .locked(record.isLocked())
                .nextConversationId(correct || record.isLocked()
                        ? npc.getNextConversationId()
                        : null)
                .correctAnswer(record.isLocked() && !correct
                        ? npc.getAnswers()
                        : null)
                .explanation(record.isLocked() && !correct
                        ? npc.getExplanation()
                        : null)
                .build();

    }

    private boolean checkAnswer(NPCConversation npc, List<String> userAnswer) {

        // 초급 / 중급 / 고급 공통 처리
        List<String> correct = npc.getAnswers();

        if (npc.getLevel() == Level.BEGINNER) {
            return correct.contains(userAnswer.get(0));
        }

        if (npc.getLevel() == Level.INTERMEDIATE) {
            return String.join(" ", correct)
                    .equalsIgnoreCase(String.join(" ", userAnswer));
        }

        // 고급
        return String.join(" ", correct)
                .equalsIgnoreCase(String.join(" ", userAnswer).trim());
    }
}
