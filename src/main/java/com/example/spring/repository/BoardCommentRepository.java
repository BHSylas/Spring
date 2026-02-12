package com.example.spring.repository;

import com.example.spring.entity.BoardComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {
    Page<BoardComment> findByBoardBoardIdAndDepthAndDeletedFalse(Long boardId, int depth, Pageable pageable);

    List<BoardComment> findByBoardBoardIdAndDepthAndDeletedFalseOrderByCreatedAtAsc(Long boardId, int depth);

    List<BoardComment> findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(Long parentId);
}
