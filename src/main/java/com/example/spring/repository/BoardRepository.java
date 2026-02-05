package com.example.spring.repository;

import com.example.spring.entity.Board;
import com.example.spring.entity.BoardType;
import com.example.spring.entity.Lecture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByBoardTypeAndDeletedFalse(BoardType boardType);

    List<Board> findByBoardTypeAndLectureAndDeletedFalse(BoardType boardType, Lecture lecture);

    Optional<Board> findByBoardIdAndDeletedFalse(Long boardId);

    List<Board> findByBoardTypeAndDeletedFalseOrderByPinnedDescCreatedAtDesc(BoardType boardType);

    List<Board> findByBoardTypeAndLectureAndDeletedFalseOrderByCreatedAtDesc(BoardType boardType, Lecture lecture);

    @Query("""
        select b from Board b where b.deleted = false
                AND(:boardType is null or b.boardType = :boardType)
                AND(:lectureId is null or b.lecture.lectureId = :lectureId)
                AND(:writerName is null or b.writer.userNickname like %:writerName%)
                AND(:title is null or b.title like %:title%)
                 """)
    Page<Board> searchBoard(@Param("boardType") BoardType boardType,
                            @Param("lectureId")Long lectureId,
                            @Param("writerName") String writerName,
                            @Param("title") String title,
                            Pageable pageable);

}
