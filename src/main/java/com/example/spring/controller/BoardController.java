package com.example.spring.controller;

import com.example.spring.dto.BoardRequestDTO;
import com.example.spring.dto.BoardResponseDTO;
import com.example.spring.entity.BoardType;
import com.example.spring.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
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

//    @GetMapping("/list")
//    public ResponseEntity<List<BoardResponseDTO>> boardList(@RequestBody BoardRequestDTO boardRequestDTO) {
//        return ResponseEntity.ok(boardService.boardList(boardRequestDTO.getBoardType(), boardRequestDTO.getLectureId()));
//    }

    @GetMapping("/searchBoard")
    public ResponseEntity<Page<BoardResponseDTO>> searchBoard(@RequestParam(required = false) BoardType boardType,
                                                              @RequestParam(required = false) Long lectureId,
                                                              @RequestParam(required = false) String writer,
                                                              @RequestParam(required = false) String title,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt")));
        return ResponseEntity.ok(boardService.searchBoard(boardType, lectureId, writer, title, pageable));
    }

    @GetMapping("/list/{boardId}")
    public ResponseEntity<BoardResponseDTO> board(@PathVariable Long boardId) {
        return ResponseEntity.ok(boardService.board(boardId));
    }

    //QnA 답변표시
    @PatchMapping("/answered/{boardId}")
    public ResponseEntity<Void> answered(@AuthenticationPrincipal Long userId, @PathVariable Long boardId) {
        boardService.markAnswered(userId, boardId);
        return ResponseEntity.ok().build();
    }

    //관리자 공지사항 고정
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("{boardId}/pin")
    public ResponseEntity<Void> pinNotice(@AuthenticationPrincipal Long userId, @PathVariable Long boardId,
                                          @RequestParam boolean pinned) {
        boardService.pinNotice(userId, boardId, pinned);
        return ResponseEntity.ok().build();
    }
}
