package com.example.spring.repository;

import com.example.spring.dto.LectureListItemDTO;
import com.example.spring.entity.EnrollmentStatus;
import com.example.spring.entity.Lecture;
import com.example.spring.entity.LectureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    // =========================================================
    // A) 관리자 대시보드/집계
    // =========================================================

    long countByStatus(LectureStatus status);

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    long countByStatusAndCreatedAtGreaterThanEqual(LectureStatus status, LocalDateTime since);

    /** 상태별 "영상 없는" 강의 수 (예: PENDING인데 영상 없음) */
    @Query("""
        select count(l)
        from Lecture l
        where l.status = :status
          and not exists (
              select 1
              from LectureVideo v
              where v.lecture = l
          )
        """)
    long countByStatusAndNoVideo(@Param("status") LectureStatus status);

    // =========================================================
    // B) 관리자/강사 관리용 조회
    // =========================================================

    /** 관리자: 상태별 강의 목록 */
    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    Page<Lecture> findByStatus(LectureStatus status, Pageable pageable);

    /** 관리자: 특정 상태인데 영상 없는 강의 목록 (요구사항: PENDING + no video 필터) */
    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    @Query("""
        select l
        from Lecture l
        where l.status = :status
          and not exists (
              select 1
              from LectureVideo v
              where v.lecture = l
          )
        """)
    Page<Lecture> findByStatusAndNoVideo(@Param("status") LectureStatus status, Pageable pageable);

    /** 교수: 본인 강의 목록 */
    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    Page<Lecture> findByProfessor_UserId(Long professorId, Pageable pageable);

    /** 교수: 본인 강의 + 상태 필터 */
    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    Page<Lecture> findByProfessor_UserIdAndStatus(Long professorId, LectureStatus status, Pageable pageable);

    // =========================================================
    // C) 학생 공개 목록/검색 (APPROVED 전용)
    //
    // 규칙:
    // - Service에서 language/keyword 전처리:
    //   * null/빈문자/ALL => null로 변환하여 전달
    // - enrolling=null이면 searchApproved만 호출
    // - enrolling!=null이면 searchApprovedByEnrollment 호출
    // =========================================================

    /** enrolling 필터 없음 */
    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    @Query("""
        select l
        from Lecture l
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
     * enrolling 필터 포함 버전 (exists/not exists)
     *
     * 주의:
     * - Enrollment에서 CANCELED 제외하려면 아래 where에 조건 추가해야 함.
     *   예: and e.status <> :excluded
     * - 지금 서비스는 EnrollmentRepository에서 activeLectureIds로 CANCELED 제외를 하고 있었는데,
     *   여기 방식으로 통일하면 Repository 하나로 끝낼 수 있음.
     */
    @EntityGraph(attributePaths = {"professor", "approvedBy"})
    @Query("""
        select l
        from Lecture l
        where l.status = :status
          and (:language is null or l.language = :language)
          and (:keyword is null
               or l.title like concat('%', :keyword, '%')
               or l.description like concat('%', :keyword, '%'))
          and (
                (:enrolling = true and exists (
                    select 1
                    from Enrollment e
                    where e.user.userId = :userId
                      and e.lecture = l
                      and e.status <> :excludedStatus
                ))
             or (:enrolling = false and not exists (
                    select 1
                    from Enrollment e
                    where e.user.userId = :userId
                      and e.lecture = l
                      and e.status <> :excludedStatus
                ))
          )
        """)
    Page<Lecture> searchApprovedByEnrollment(
            @Param("status") LectureStatus status,
            @Param("userId") Long userId,
            @Param("enrolling") boolean enrolling,
            @Param("excludedStatus") EnrollmentStatus excludedStatus,
            @Param("language") String language,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // =========================================================
    // D) 목록 전용 DTO (썸네일까지 포함) - 모든 이용자 공통 카드용
    // =========================================================

    /** 교수: 내 강의 목록 카드(썸네일 포함) - status가 null이면 전체 */
    @Query("""
    select new com.example.spring.dto.LectureListItemDTO(
        l.lectureId,
        l.title,
        l.country,
        l.language,
        l.status,
        p.userId,
        p.userNickname,
        v.sourceType,
        v.thumbnailUrl,
        v.durationSec,
        v.youtubeVideoTitle,
        v.youtubeChannelTitle,
        l.createdAt
    )
    from Lecture l
    join l.professor p
    left join LectureVideo v on v.lecture = l
    where p.userId = :professorId
      and (:status is null or l.status = :status)
    """)
    Page<LectureListItemDTO> findMyLectureCardItems(
            @Param("professorId") Long professorId,
            @Param("status") LectureStatus status,
            Pageable pageable
    );

    /** 관리자: 강의 목록 카드(썸네일 포함) - status가 null이면 전체 */
    @Query("""
    select new com.example.spring.dto.LectureListItemDTO(
        l.lectureId,
        l.title,
        l.country,
        l.language,
        l.status,
        p.userId,
        p.userNickname,
        v.sourceType,
        v.thumbnailUrl,
        v.durationSec,
        v.youtubeVideoTitle,
        v.youtubeChannelTitle,
        l.createdAt
    )
    from Lecture l
    join l.professor p
    left join LectureVideo v on v.lecture = l
    where (:status is null or l.status = :status)
    """)
    Page<LectureListItemDTO> adminLectureCardItems(
            @Param("status") LectureStatus status,
            Pageable pageable
    );

    @Query("""
    select new com.example.spring.dto.LectureListItemDTO(
        l.lectureId,
        l.title,
        l.country,
        l.language,
        l.status,
        p.userId,
        p.userNickname,
        v.sourceType,
        v.thumbnailUrl,
        v.durationSec,
        v.youtubeVideoTitle,
        v.youtubeChannelTitle,
        l.createdAt
    )
    from Lecture l
    join l.professor p
    left join LectureVideo v on v.lecture = l
    where l.status = :status
      and not exists (
          select 1
          from LectureVideo v2
          where v2.lecture = l
      )
    """)
    Page<LectureListItemDTO> adminPendingWithoutVideoCardItems(
            @Param("status") LectureStatus status,
            Pageable pageable
    );

    /**
     * 학생: 승인 강의 목록 카드(썸네일 포함)
     * enrolling == null => 전체
     * enrolling != null => 로그인 필요(서비스에서 검증)
     */
    @Query("""
    select new com.example.spring.dto.LectureListItemDTO(
        l.lectureId,
        l.title,
        l.country,
        l.language,
        l.status,
        p.userId,
        p.userNickname,
        v.sourceType,
        v.thumbnailUrl,
        v.durationSec,
        v.youtubeVideoTitle,
        v.youtubeChannelTitle,
        l.createdAt
    )
    from Lecture l
    join l.professor p
    left join LectureVideo v on v.lecture = l
    where l.status = :status
      and (:language is null or l.language = :language)
      and (:keyword is null
           or l.title like concat('%', :keyword, '%')
           or l.description like concat('%', :keyword, '%'))
      and (
            :enrolling is null
         or (
                :enrolling = true and exists (
                    select 1
                    from Enrollment e
                    where e.user.userId = :userId
                      and e.lecture = l
                      and e.status <> :excludedStatus
                )
            )
         or (
                :enrolling = false and not exists (
                    select 1
                    from Enrollment e
                    where e.user.userId = :userId
                      and e.lecture = l
                      and e.status <> :excludedStatus
                )
            )
      )
    """)
    Page<LectureListItemDTO> approvedLectureCardItems(
            @Param("status") LectureStatus status, // APPROVED 고정으로 넣어도 됨
            @Param("userId") Long userId,          // enrolling null이면 null이어도 됨
            @Param("enrolling") Boolean enrolling,
            @Param("excludedStatus") EnrollmentStatus excludedStatus,
            @Param("language") String language,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}