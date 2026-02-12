package com.example.spring.controller;

import com.example.spring.dto.BoardCommentRequestDTO;
import com.example.spring.dto.BoardCommentResponseDTO;
import com.example.spring.service.BoardCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards/comments")
public class BoardCommentController {
    private final BoardCommentService boardCommentService;

    @PostMapping("/create/{boardId}")
    public ResponseEntity<Void> create(@AuthenticationPrincipal Long userId, @PathVariable Long boardId,
                                       @RequestParam(required = false) Long parentId, @RequestBody BoardCommentRequestDTO dto) {
        boardCommentService.createComment(userId, boardId, parentId, dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/update/{commentId}")
    public ResponseEntity<Void> update(@AuthenticationPrincipal Long userId, @PathVariable Long commentId,
                                       @RequestBody BoardCommentRequestDTO dto) {
        boardCommentService.updateComment(userId, commentId, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{commentId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long commentId) {
        boardCommentService.deleteComment(userId, commentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list/{boardId}")
    public ResponseEntity<Page<BoardCommentResponseDTO>> list(@PathVariable Long boardId,
                                                              @RequestParam(defaultValue = "0")int page,
                                                              @RequestParam(defaultValue = "5")int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(boardCommentService.getComments(boardId, pageable));
    }
}
