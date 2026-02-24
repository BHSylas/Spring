package com.example.spring.repository;

import com.example.spring.entity.LectureVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LectureVideoRepository extends JpaRepository<LectureVideo, Long> {
    Optional<LectureVideo> findByLecture_LectureId(Long lectureId);
    boolean existsByLecture_LectureId(Long lectureId);

    @Query("""
    select v
    from LectureVideo v
    join fetch v.lecture l
    join fetch l.professor p
    where v.videoId = :videoId
    """)
    Optional<LectureVideo> findWithLectureAndProfessorByVideoId(@Param("videoId") Long videoId);
}
