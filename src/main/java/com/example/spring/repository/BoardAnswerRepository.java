package com.example.spring.repository;

import com.example.spring.entity.Board;
import com.example.spring.entity.BoardAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardAnswerRepository extends JpaRepository<BoardAnswer, Long> {
    List<BoardAnswer> findByBoardBoardIdAndDeletedFalse(Long boardId);

    Long board(Board board);
}
