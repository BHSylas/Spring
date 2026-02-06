package com.example.spring.repository;

import com.example.spring.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    // =========================================================
    // 1) 관리자 / 강사 관리용 조회
    // =========================================================

    /** 관리자: 상태별 강의 목록 */
    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    Page<Lecture> findByStatus(LectureStatus status, Pageable pageable);

    /** 교수: 본인 강의 목록 */
    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    Page<Lecture> findByProfessor_UserId(Long professorId, Pageable pageable);

    /** 교수: 본인 강의 + 상태 필터 */
    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    Page<Lecture> findByProfessor_UserIdAndStatus(Long professorId, LectureStatus status, Pageable pageable);


    // =========================================================
    // 2) 학생 공개 목록/검색 (APPROVED 전용)
    //
    // 규칙(중요):
    // - language / keyword는 Service에서 전처리:
    //   * ALL/빈문자열/공백 => null 로 변환해서 넘긴다
    // - Repository는 null 여부만 판단
    // =========================================================

    /**
     * 학생: 승인된 강의 검색/목록 (enrolling 필터 없음)
     * - language == null => 전체
     * - keyword == null => 검색 없음
     */
    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    @Query("""
    select l from Lecture l
    where l.status = :status
    and (:language is null or l.language = :language)
    and (:keyword is null
        or l.title like concat('%', :keyword, '%')
        or l.description like concat('%', :keyword, '%'))
    """)
    Page<Lecture> searchApproved(
            @Param("status") LectureStatus status,
            @Param("language") String language,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 학생: 승인된 강의 검색/목록 + 수강중(enrolling) 필터
     * enrolling=true  -> exists
     * enrolling=false -> not exists
     */
    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    @Query("""
        select l from Lecture l
        where l.status = :status
          and (:language is null or l.language = :language)
          and (:keyword is null
               or l.title like concat('%', :keyword, '%')
               or l.description like concat('%', :keyword, '%'))
          and (
              (:enrolling = true and exists (
                  select 1 from Enrollment e
                  where e.user.userId = :userId
                    and e.lecture.lectureId = l.lectureId
              ))
              or
              (:enrolling = false and not exists (
                  select 1 from Enrollment e
                  where e.user.userId = :userId
                    and e.lecture.lectureId = l.lectureId
              ))
          )
        """)
    Page<Lecture> searchApprovedByEnrollment(
            @Param("status") LectureStatus status,
            @Param("userId") Long userId,
            @Param("enrolling") boolean enrolling,
            @Param("language") String language,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // enrolling=true  -> lectureId IN (:lectureIds)
    // enrolling=false -> lectureId NOT IN (:lectureIds)

    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    @Query("""
    select l from Lecture l
    where l.status = :status
      and (:language is null or l.language = :language)
      and (:keyword is null
          or l.title like concat('%', :keyword, '%')
          or l.description like concat('%', :keyword, '%'))
      and l.lectureId in :lectureIds
    """)
    Page<Lecture> searchApprovedInLectureIds(
            @Param("status") LectureStatus status,
            @Param("language") String language,
            @Param("keyword") String keyword,
            @Param("lectureIds") java.util.List<Long> lectureIds,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    @Query("""
    select l from Lecture l
    where l.status = :status
      and (:language is null or l.language = :language)
      and (:keyword is null
          or l.title like concat('%', :keyword, '%')
          or l.description like concat('%', :keyword, '%'))
      and l.lectureId not in :lectureIds
    """)
    Page<Lecture> searchApprovedNotInLectureIds(
            @Param("status") LectureStatus status,
            @Param("language") String language,
            @Param("keyword") String keyword,
            @Param("lectureIds") java.util.List<Long> lectureIds,
            Pageable pageable
    );
}
