package com.example.spring.repository;

import com.example.spring.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface LectureRepository extends JpaRepository<Lecture, Long> {
//    List<Lecture> findAllByIsActiveTrue();

//    List<Lecture> findAllByProfessorId(Long professorId);

    boolean existsByIdAndApprovedTrue(Long lectureId);
}
