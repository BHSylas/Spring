package com.example.spring.repository;

import com.example.spring.entity.LectureVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LectureVideoRepository extends JpaRepository<LectureVideo, Long> {
    Optional<LectureVideo> findByLecture_LectureId(Long lectureId);
    boolean existsByLecture_LectureId(Long lectureId);
}
