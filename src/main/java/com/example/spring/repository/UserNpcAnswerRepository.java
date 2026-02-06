package com.example.spring.repository;

import com.example.spring.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNpcAnswerRepository extends JpaRepository<UserNpcAnswer, Long> {

    Optional<UserNpcAnswer> findByUserUserIdAndNpcConversation(Long userId, NPCConversation npc);

    long countByUserUserIdAndCountryAndLevel(Long userId, Country country, Level level);

    long countByUserUserIdAndCountryAndLevelAndCorrectTrue(Long userId, Country country, Level level);

    long countByNpcConversation_Professor_UserIdAndCountryAndLevelAndCorrectTrue(
            Long professorId, Country country, Level level);

    long countByCountryAndLevel(Country country, Level level);

    long countByCountryAndLevelAndCorrectTrue(Country country, Level level);


}
