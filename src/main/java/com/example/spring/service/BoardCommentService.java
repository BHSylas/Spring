package com.example.spring.service;

import com.example.spring.dto.BoardCommentRequestDTO;
import com.example.spring.dto.BoardCommentResponseDTO;
import com.example.spring.entity.Board;
import com.example.spring.entity.BoardComment;
import com.example.spring.entity.User;
import com.example.spring.repository.BoardCommentRepository;
import com.example.spring.repository.BoardRepository;
import com.example.spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardCommentService {
    private final BoardRepository boardRepository;
    private final BoardCommentRepository boardCommentRepository;
    private final UserRepository userRepository;

    public void createComment(Long userId, Long boardId, Long parentId, BoardCommentRequestDTO requestDTO) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자 정보 없음"));

        BoardComment parent = null;
        int depth = 0;

        if(parentId != null) {
            parent = boardCommentRepository.findById(parentId).orElseThrow(() -> new IllegalArgumentException("부모 댓글 없음"));

            if(parent.getDepth() == 1){
                throw new IllegalStateException("대댓글에는 댓글 불가");
            }

            depth = 1;
        }

        BoardComment boardComment = BoardComment.builder()
                .board(board)
                .writer(user)
                .parent(parent)
                .depth(depth)
                .content(requestDTO.getContent())
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        boardCommentRepository.save(boardComment);
        board.increaseCommentCount();
    }

    @Transactional
    public void updateComment(Long userId, Long commentId, BoardCommentRequestDTO requestDTO) {

        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));

        if (comment.isDeleted()) {
            throw new IllegalStateException("삭제된 댓글은 수정 불가");
        }

        if(!comment.getWriter().getUserId().equals(userId)) {
            throw new IllegalStateException("수정 권한 없음");
        }

        comment.update(requestDTO.getContent());
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));

        Board board = comment.getBoard();

        if(!comment.getWriter().getUserId().equals(userId)) {
            throw new IllegalStateException("삭제 권한 없음");
        }

        if(comment.getDepth() == 0 && !comment.getChildren().isEmpty()) {
            comment.update("삭제된 댓글입니다.");
            comment.delete();
            return;
        }

        comment.delete();
        board.decreaseCommentCount();
    }

    @Transactional(readOnly = true)
    public Page<BoardCommentResponseDTO> getComments(Long boardId, Pageable pageable) {
        Page<BoardComment> parentsPage = boardCommentRepository.findByBoardBoardIdAndDepthAndDeletedFalse(boardId, 0, pageable);

        return parentsPage.map(this::toDTOWithChildren);
    }


    private BoardCommentResponseDTO toDTOWithChildren(BoardComment parent){
        List<BoardCommentResponseDTO> children = boardCommentRepository.findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(parent.getCommentId())
                .stream()
                .map(child -> BoardCommentResponseDTO.builder()
                        .commentId(child.getCommentId())
                        .writerId(child.getWriter().getUserId())
                        .writerName(child.getWriter().getUserNickname())
                        .content(child.isDeleted() ? "삭제된 댓글입니다." : child.getContent())
                        .depth(child.getDepth())
                        .createdAt(child.getCreatedAt())
                        .children(List.of())
                        .build())
                .toList();


        return BoardCommentResponseDTO.builder()
                .commentId(parent.getCommentId())
                .writerId(parent.getWriter().getUserId())
                .writerName(parent.getWriter().getUserNickname())
                .content(parent.isDeleted() ? "삭제된 댓글입니다." : parent.getContent())
                .depth(parent.getDepth())
                .createdAt(parent.getCreatedAt())
                .children(children)
                .build();
    }


}
