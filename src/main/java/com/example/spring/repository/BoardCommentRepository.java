package com.example.spring.repository;

import com.example.spring.entity.BoardComment;
import com.example.spring.entity.BoardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {
    Page<BoardComment> findByBoardBoardIdAndDepthAndDeletedFalse(Long boardId, int depth, Pageable pageable);

    List<BoardComment> findByBoardBoardIdAndDepthAndDeletedFalseOrderByCreatedAtAsc(Long boardId, int depth);

    List<BoardComment> findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(Long parentId);

    long countByBoardBoardIdAndDeletedFalse(Long boardId);

    @Query("""
        select c from BoardComment c
        join fetch c.board b
        where c.writer.userId = :userId
          and c.deleted = false
          and b.boardType != :excludeType
        order by c.createdAt desc
        """)
    Page<BoardComment> findByWriterWithBoard(
            @Param("userId") Long userId,
            @Param("excludeType") BoardType excludeType,
            Pageable pageable
    );
}
