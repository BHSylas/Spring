package com.example.spring.controller;

import com.example.spring.dto.BoardAnswerRequestDTO;
import com.example.spring.dto.BoardAnswerResponseDTO;
import com.example.spring.service.BoardAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards/qna/answers")
public class BoardAnswerController {
    private final BoardAnswerService boardAnswerService;

    @PostMapping("/create/{boardId}")
    public ResponseEntity<Long> create(@AuthenticationPrincipal Long professorId, @PathVariable Long boardId,
                                       @RequestBody BoardAnswerRequestDTO boardAnswerRequestDTO){
        return ResponseEntity.ok(boardAnswerService.createAnswer(professorId, boardId, boardAnswerRequestDTO));

    }

    @PutMapping("/update/{answerId}")
    public ResponseEntity<Void> update(@PathVariable Long answerId, @AuthenticationPrincipal Long professorId,
                                       @RequestBody BoardAnswerRequestDTO boardAnswerRequestDTO){
        boardAnswerService.updateAnswer(answerId, professorId, boardAnswerRequestDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{answerId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long professorId, @PathVariable Long answerId){
        boardAnswerService.deleteAnswer(professorId, answerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("list/{boardId}")
    public ResponseEntity<List<BoardAnswerResponseDTO>> list(@PathVariable Long boardId){
        return ResponseEntity.ok(boardAnswerService.getAnswers(boardId));
    }
}
