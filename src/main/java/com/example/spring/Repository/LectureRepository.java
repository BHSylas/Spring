package com.example.spring.Repository;

import com.example.spring.Entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface LectureRepository extends JpaRepository<Lecture, Long> {
    List<Lecture> findAllByIsActiveTrue();

    List<Lecture> findAllByProfessorId(Long professorId);

    boolean existsByIdAndApprovedTrue(Long lectureId);
}
