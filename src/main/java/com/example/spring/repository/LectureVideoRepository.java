package com.example.spring.repository;

import com.example.spring.entity.LectureVideo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LectureVideoRepository extends JpaRepository<LectureVideo, Long> {
    Optional<LectureVideo> findByLecture_LectureId(Long lectureId);
    boolean existsByLecture_LectureId(Long lectureId);

    /** 스트리밍 권한 체크를 위해 lecture/professor까지 한 번에 로딩 (N+1 방지) */
    @EntityGraph(attributePaths = {"lecture", "lecture.professor"})
    Optional<LectureVideo> findWithLectureAndProfessorByVideoId(Long videoId);
}
