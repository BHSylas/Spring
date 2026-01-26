package com.example.spring.repository;

import com.example.spring.entity.Enrollment;
import com.example.spring.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    // 유저 + 강의 수강 정보 (핵심 ⭐)
    Optional<Enrollment> findByUserIdAndLectureId(Long userId, Long lectureId);

    // 유저가 수강 중인 모든 강의
    List<Enrollment> findAllByUserId(Long userId);

    // 특정 강의 수강생 목록
    List<Enrollment> findAllByLectureId(Long lectureId);

    // 유저 + 상태별 강의 조회 (진행중 / 완료)
    List<Enrollment> findAllByUserIdAndStatus(Long userId, EnrollmentStatus status);

    // 강의 + 상태별 수강생 조회 (교수용)
    List<Enrollment> findAllByLectureIdAndStatus(Long lectureId, EnrollmentStatus status);

    // 수강 여부 체크
    boolean existsByUserIdAndLectureId(Long userId, Long lectureId);

}
