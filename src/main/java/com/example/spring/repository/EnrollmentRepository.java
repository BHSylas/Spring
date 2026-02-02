package com.example.spring.repository;

import com.example.spring.entity.Enrollment;
import com.example.spring.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    // 유저 + 강의 수강 정보
    Optional<Enrollment> findByUser_UserIdAndLecture_LectureId(Long userId, Long lectureId);

    boolean existsByUser_UserIdAndLecture_LectureId(Long userId, Long lectureId);

    Page<Enrollment> findAllByUser_UserId(Long userId, Pageable pageable);

    Page<Enrollment> findAllByLecture_LectureId(Long lectureId, Pageable pageable);

    // 강의의 수강 목록(상태별)
    Page<Enrollment> findAllByLecture_LectureIdAndStatus(Long lectureId, EnrollmentStatus status, Pageable pageable);

    //  내가 수강중인 lectureId 목록(필터링용)
    @Query("select e.lecture.lectureId from Enrollment e where e.user.userId = :userId")
    List<Long> findLectureIdsByUserId(@Param("userId") Long userId);

}
