package com.example.spring.service;

import com.example.spring.dto.ConversationResponseDTO;
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

@Service
@RequiredArgsConstructor
public class NpcConversationService {

    private final NpcConversationRepository conversationRepository;
    private final UserNpcAnswerRepository answerRepository;

    @Transactional
    public ConversationResponseDTO submitAnswer(
            User user,
            Long conversationId,
            String userAnswer
    ) {
        NPCConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow();

        boolean isCorrect = checkAnswer(conversation, userAnswer);

        answerRepository.save(
                UserNpcAnswer.builder()
                        .userId(user.getUserId())
                        .conversationId(conversationId)
                        .answer(userAnswer)
                        .correct(isCorrect)
                        .answeredAt(LocalDateTime.now())
                        .build()
        );

        return ConversationResponseDTO.builder()
                .correct(isCorrect)
                .nextConversationId(
                        isCorrect ? conversation.getNextConversationId() : null
                )
                .explanation(
                        isCorrect ? null : conversation.getExplanation()
                )
                .build();
    }

    private boolean checkAnswer(NPCConversation conversation, String userAnswer) {

        // 고급: 주관식
        if (conversation.getLevel() == Level.ADVANCED) {
            return conversation.getAnswers().stream()
                    .anyMatch(a ->
                            a.equalsIgnoreCase(userAnswer.trim())
                    );
        }

        // 초급 / 중급
        return conversation.getAnswers().contains(userAnswer);
    }
}
