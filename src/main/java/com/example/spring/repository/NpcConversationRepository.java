package com.example.spring.repository;

import com.example.spring.entity.Country;
import com.example.spring.entity.Level;
import com.example.spring.entity.NPCConversation;
import com.example.spring.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NpcConversationRepository extends JpaRepository<NPCConversation, Long> {

    Optional<NPCConversation> findByNpcIdAndProfessorUserId(Long npcId, Long professorId);

    List<NPCConversation> findByLecture_LectureId(Long lectureId);

    void deleteByLecture_LectureId(Long lectureId);

    boolean existsByLecture_LectureId(Long lectureId);

    // 교수용: 본인 NPC 목록 필터 조회
    @Query("""
        select n from NPCConversation n where n.professor.userId = :professorId
                AND (:country is null or n.country = :country)
                AND (:level IS NULL or n.level = :level)
                AND (:place IS NULL or n.place = :place)
                AND n.active = true
        """)
    Page<NPCConversation> findByProfessorWithFilter(@Param("professorId") Long professorId,
                                                    @Param("country") Country country,
                                                    @Param("level") Level level,
                                                    @Param("place") Place place,
                                                    Pageable pageable);

    // 다음 대화 후보 조회 (교수용)
    @Query("""
        select n from NPCConversation n where  n.lecture.lectureId = :lectureId
                AND n.country = :country
                AND n.place = :place
                AND n.level = :level
                AND n.active = true
                AND n.npcId <> :currentNpcId
        """)
    List<NPCConversation> findNextCandidate(Long lectureId, Country country,Place place, Level level,
             Long currentNpcId);


    // =========================================================
    // 학생용: 강의 독립 전체 활성 NPC 조회 (필터 선택 적용)
    // country / place / level 이 null 이면 전체 조회
    // =========================================================
    @Query("""
        select n from NPCConversation n
        where n.active = true
          and (:country is null or n.country = :country)
          and (:place   is null or n.place   = :place)
          and (:level   is null or n.level   = :level)
        order by n.country asc, n.level asc, n.place asc, n.npcId asc
        """)
    List<NPCConversation> findAllActiveWithFilter(
            @Param("country") Country country,
            @Param("place")   Place place,
            @Param("level")   Level level
    );



    long countByCountryAndLevel(Country country, Level level);

    long countByProfessor_UserIdAndCountryAndLevel(Long professorId, Country country, Level level );

}
