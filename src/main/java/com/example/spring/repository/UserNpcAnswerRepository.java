package com.example.spring.repository;

import com.example.spring.entity.NPCConversation;
import com.example.spring.entity.User;
import com.example.spring.entity.UserNpcAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNpcAnswerRepository extends JpaRepository<UserNpcAnswer, Long> {
    Optional<UserNpcAnswer> findByUserUserIdAndNpcConversationId(Long userId, Long npcId);

    Optional<UserNpcAnswer> findByUserAndNpcConversation(User user, NPCConversation npc);
    Optional<UserNpcAnswer> findTopByUserAndNpcConversationOrderByAnsweredAtDesc(User user, NPCConversation npcConversation);
    int countByUserAndNpcConversation(User user, NPCConversation npc);
}
