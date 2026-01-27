package com.example.spring.repository;

import com.example.spring.entity.Lecture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface LectureRepository extends JpaRepository<Lecture, Long> {
    List<Lecture> findAllByApprovedTrue();

    List<Lecture> findAllByProfessor_UserId(Long professorId);

    boolean existsByIdAndApprovedTrue(Long lectureId);

    //전체 강의 조회
    Page<Lecture> findByLanguage(String language, Pageable pageable);

    //수강중
    Page<Lecture> findByIdIn(List<Long> ids, Pageable pageable);
    Page<Lecture> findByIdInAndLanguage(List<Long> ids, String language, Pageable pageable);

    //미수강
    Page<Lecture> findByIdNotIn(List<Long> ids, Pageable pageable);
    Page<Lecture> findByIdNotInAndLanguage(List<Long> ids, String language, Pageable pageable);
}
