package com.example.spring.controller;

import com.example.spring.dto.BoardRequestDTO;
import com.example.spring.dto.BoardResponseDTO;
import com.example.spring.entity.BoardType;
import com.example.spring.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board")
public class BoardController {
    private final BoardService boardService;

    @PostMapping("/create")
    public ResponseEntity<Long> create(@AuthenticationPrincipal Long userId, @RequestBody BoardRequestDTO boardCreateRequestDTO) {
        return ResponseEntity.ok(boardService.create(userId, boardCreateRequestDTO));
    }

    @PutMapping("/update/{boardId}")
    public ResponseEntity<Void> update(@AuthenticationPrincipal Long userId, @PathVariable Long boardId, @RequestBody BoardRequestDTO boardUpdateRequestDTO) {
        boardService.update(userId, boardId, boardUpdateRequestDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{boardId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long boardId) {
        boardService.delete(userId, boardId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<BoardResponseDTO>> boardList(@RequestBody BoardRequestDTO boardRequestDTO) {
        return ResponseEntity.ok(boardService.boardList(boardRequestDTO.getBoardType(), boardRequestDTO.getLectureId()));

    }

    @GetMapping("/list/{boardId}")
    public ResponseEntity<BoardResponseDTO> board(@PathVariable Long boardId) {
        return ResponseEntity.ok(boardService.board(boardId));
    }

    @PatchMapping("/answered/{boardId}")
    public ResponseEntity<Void> answered(@AuthenticationPrincipal Long userId, @PathVariable Long boardId) {
        boardService.markAnswered(userId, boardId);
        return ResponseEntity.ok().build();
    }
}
