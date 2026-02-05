package com.example.spring.repository;

import com.example.spring.entity.Board;
import com.example.spring.entity.BoardType;
import com.example.spring.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByBoardTypeAndDeletedFalse(BoardType boardType);

    List<Board> findByBoardTypeAndLectureAndDeletedFalse(BoardType boardType, Lecture lecture);

    Optional<Board> findByBoardIdAndDeletedFalse(Long boardId);

    List<Board> findByBoardTypeAndDeletedFalseOrderByPinnedDescCreatedAtDesc(
            BoardType boardType
    );

    List<Board> findByBoardTypeAndLectureAndDeletedFalseOrderByCreatedAtDesc(
            BoardType boardType, Lecture lecture
    );
}
