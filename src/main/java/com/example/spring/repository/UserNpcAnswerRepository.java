package com.example.spring.repository;

import com.example.spring.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserNpcAnswerRepository extends JpaRepository<UserNpcAnswer, Long> {

    Optional<UserNpcAnswer> findByUserUserIdAndNpcConversation(Long userId, NPCConversation npc);

    boolean existsByNpcConversation_Lecture_LectureId(Long lectureId);

    long countByUserUserIdAndCountryAndLevel(Long userId, Country country, Level level);

    long countByUserUserIdAndCountryAndLevelAndCorrectTrue(Long userId, Country country, Level level);

    long countByNpcConversation_Professor_UserIdAndCountryAndLevelAndCorrectTrue(
            Long professorId, Country country, Level level);

    long countByNpcConversation_Professor_UserIdAndCountryAndLevel(
            Long professorId, Country country, Level level);

    long countByCountryAndLevel(Country country, Level level);

    long countByCountryAndLevelAndCorrectTrue(Country country, Level level);


    // =========================================================
    // 학생용: npcId 목록에 해당하는 답변 기록 일괄 조회 (N+1 방지)
    // =========================================================
    @Query("""
        select a from UserNpcAnswer a
        where a.user.userId = :userId
          and a.npcConversation.npcId in :npcIds
        """)
    List<UserNpcAnswer> findByUserUserIdAndNpcConversationNpcIdIn(
            @Param("userId") Long userId,
            @Param("npcIds") List<Long> npcIds);


    @Query("""
    select count(distinct a.user.userId)
    from UserNpcAnswer a
    where a.country = :country
      and a.level = :level
    """)
    long countDistinctUserByCountryAndLevel(
            @Param("country") Country country,
            @Param("level") Level level
    );

    @Query("""
        select count(distinct a.user.userId)
        from UserNpcAnswer a
        where a.npcConversation.professor.userId = :professorId
          and a.country = :country
          and a.level = :level
        """)
    long countDistinctUserByProfessorAndCountryAndLevel(
            @Param("professorId") Long professorId,
            @Param("country") Country country,
            @Param("level") Level level
    );

}


