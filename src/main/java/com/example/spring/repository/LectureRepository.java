package com.example.spring.repository;

import com.example.spring.entity.Lecture;
import com.example.spring.entity.LectureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;


public interface LectureRepository extends JpaRepository<Lecture, Long> {
    Page<Lecture> findByStatus(LectureStatus status, Pageable pageable);

    Page<Lecture> findByProfessor_UserId(Long professorId, Pageable pageable);

    Page<Lecture> findByStatusAndLanguage(LectureStatus status, String language, Pageable pageable);

    Page<Lecture> findByStatusAndLectureIdIn(LectureStatus status, Collection<Long> lectureIds, Pageable pageable);
    Page<Lecture> findByStatusAndLectureIdNotIn(LectureStatus status, Collection<Long> lectureIds, Pageable pageable);

    Page<Lecture> findByStatusAndLanguageAndLectureIdIn(LectureStatus status, String language, Collection<Long> lectureIds, Pageable pageable);
    Page<Lecture> findByStatusAndLanguageAndLectureIdNotIn(LectureStatus status, String language, Collection<Long> lectureIds, Pageable pageable);
}
