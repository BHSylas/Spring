package com.example.spring.service;

import com.example.spring.dto.AnswerSubmitRequestDTO;
import com.example.spring.dto.AnswerSubmitResponseDTO;
import com.example.spring.entity.Level;
import com.example.spring.entity.NPCConversation;
import com.example.spring.entity.User;
import com.example.spring.entity.UserNpcAnswer;
import com.example.spring.repository.NpcConversationRepository;
import com.example.spring.repository.UserNpcAnswerRepository;
import com.example.spring.repository.UserRepository;
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
    private final UserRepository userRepository;

    public AnswerSubmitResponseDTO answerSubmit(Long userId, Long npcConversationId, String userAnswer) {
        NPCConversation npc = npcConversationRepository.findById(npcConversationId).orElseThrow(()
        -> new IllegalArgumentException("NPC 대화 없음"));

        User user = userRepository.findById(userId).orElseThrow(() ->
                new IllegalArgumentException("사용자 없음"));

        UserNpcAnswer record = userNpcAnswerRepository
                .findByUserUserIdAndNpcConversation(userId, npc)
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

    // =========================================================
    // 레벨별 정답 체크
    // =========================================================

    private boolean checkAnswer(NPCConversation npc, String userAnswer) {
        return switch (npc.getLevel()) {
            case BEGINNER     -> checkBeginner(npc, userAnswer);
            case INTERMEDIATE -> checkIntermediate(npc, userAnswer);
            case ADVANCED     -> checkAdvanced(npc, userAnswer);
        };
    }

    /**
     * 초급: 빈칸에 들어갈 단어 하나를 선택지에서 고름
     * answers 리스트 중 하나와 일치하면 정답
     * ex) answers = ["go", "going"] → "go" 제출 시 정답
     */
    private boolean checkBeginner(NPCConversation npc, String userAnswer) {
        if (userAnswer == null) return false;
        String trimmed = userAnswer.trim();
        return npc.getAnswers().stream()
                .anyMatch(a -> a.equalsIgnoreCase(trimmed));
    }

    /**
     * 중급: options에 있는 단어들을 순서대로 조합해 문장을 만듦
     * 프론트에서 단어 순서대로 공백으로 이어 붙여 전송
     * ex) answers = ["I would like a coffee"] → "I would like a coffee" 제출 시 정답
     *     순서가 틀리면 ("coffee a like would I") 오답
     */
    private boolean checkIntermediate(NPCConversation npc, String userAnswer) {
        if (userAnswer == null) return false;
        String submitted = normalize(userAnswer);
        return npc.getAnswers().stream()
                .anyMatch(a -> normalize(a).equals(submitted));
    }

    /**
     * 고급: 사용자가 문장을 직접 입력
     * answers 리스트 중 하나와 일치하면 정답 (대소문자 무시, 앞뒤 공백 제거)
     * ex) answers = ["I'd like a coffee, please", "I would like a coffee, please"]
     *     → 둘 중 하나와 일치하면 정답
     */
    private boolean checkAdvanced(NPCConversation npc, String userAnswer) {
        if (userAnswer == null) return false;
        String submitted = normalize(userAnswer);
        return npc.getAnswers().stream()
                .anyMatch(a -> normalize(a).equals(submitted));
    }

    /**
     * 공백 정규화 + 대소문자 통일
     * "  I  would   like  " → "i would like"
     */
    private String normalize(String s) {
        if (s == null) return "";
        return s.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");  // 연속 공백을 단일 공백으로
    }
}
