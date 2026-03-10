package com.example.spring.controller;

import com.example.spring.dto.AnswerSubmitRequestDTO;
import com.example.spring.dto.AnswerSubmitResponseDTO;
import com.example.spring.entity.User;
import com.example.spring.service.AnswerSubmitService;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class AnswerSubmitController {

    private final AnswerSubmitService answerSubmitService;
    @PostMapping("/answer/{ConversationId}")
    public ResponseEntity<AnswerSubmitResponseDTO> submitAnswer(@AuthenticationPrincipal Long userId, @PathVariable Long ConversationId,
                                                                @RequestBody AnswerSubmitRequestDTO answerSubmitRequestDTO) {
        return ResponseEntity.ok(answerSubmitService.answerSubmit(userId,ConversationId,answerSubmitRequestDTO.getUserAnswer()));
    }

    @DeleteMapping("/answer/{conversationId}/reset")
    public ResponseEntity<Void> resetAnswer(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long conversationId) {
        answerSubmitService.resetAnswer(userId, conversationId);
        return ResponseEntity.ok().build();
    }
}
