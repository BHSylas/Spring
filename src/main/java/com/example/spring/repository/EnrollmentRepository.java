package com.example.spring.repository;

import com.example.spring.entity.Enrollment;
import com.example.spring.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // 유저 + 강의 수강 정보
    Optional<Enrollment> findByUser_UserIdAndLecture_LectureId(Long userId, Long lectureId);

    boolean existsByUser_UserIdAndLecture_LectureId(Long userId, Long lectureId);

    // 내 수강 목록
    Page<Enrollment> findAllByUser_UserId(Long userId, Pageable pageable);
    Page<Enrollment> findAllByUser_UserIdAndStatus(Long userId, EnrollmentStatus status, Pageable pageable);

    // sort=title 대응 (lecture.title 정렬은 Pageable Sort로 하면 깨질 수 있어 전용 메서드로 분리)
    Page<Enrollment> findByUser_UserIdOrderByLecture_TitleAscEnrollmentIdDesc(Long userId, Pageable pageable);
    Page<Enrollment> findByUser_UserIdAndStatusOrderByLecture_TitleAscEnrollmentIdDesc(Long userId, EnrollmentStatus status, Pageable pageable);

    // 강의의 수강 목록(상태별)
    Page<Enrollment> findAllByLecture_LectureId(Long lectureId, Pageable pageable);
    Page<Enrollment> findAllByLecture_LectureIdAndStatus(Long lectureId, EnrollmentStatus status, Pageable pageable);

    // 내가 수강했던 lectureId 목록(취소 포함) - (과거 호환용)
    @Query("select e.lecture.lectureId from Enrollment e where e.user.userId = :userId")
    List<Long> findLectureIdsByUserId(@Param("userId") Long userId);

    // 내가 “현재 활성 수강중”인 lectureId 목록(CANCELED 제외) - enrolling 필터용
    @Query("""
        select e.lecture.lectureId
        from Enrollment e
        where e.user.userId = :userId
          and e.status <> :excluded
    """)
    List<Long> findActiveLectureIdsByUserId(@Param("userId") Long userId,
                                            @Param("excluded") EnrollmentStatus excluded);

    // 강사 통계용 (CANCELED 제외)
    long countByLecture_LectureIdAndStatusNot(Long lectureId, EnrollmentStatus status);
    long countByLecture_LectureIdAndStatus(Long lectureId, EnrollmentStatus status);

    @Query("""
        select coalesce(avg(e.progressRate), 0)
        from Enrollment e
        where e.lecture.lectureId = :lectureId
          and e.status <> :excluded
    """)
    Double avgProgressRateExcludeStatus(@Param("lectureId") Long lectureId,
                                        @Param("excluded") EnrollmentStatus excluded);
}
