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

    @Query("""
        select n from NPCConversation n where  n.lecture.lectureId = :lectureId
                AND n.country = :country
                AND n.place = :place
                AND n.level = :level
                AND n.active = true
                AND n.npcId <> : currentNpcId
        """)
    List<NPCConversation> findNextCandidate(Long lectureId, Country country,Place place, Level level,
             Long currentNpdId);


    long countByCountryAndLevel(Country country, Level level);

    long countByProfessor_UserIdAndCountryAndLevel(Long professorId, Country country, Level level );

}
