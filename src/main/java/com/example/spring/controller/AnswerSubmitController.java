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

    AnswerSubmitService answerSubmitService;
    @PostMapping("/answer/{ConversationId}")
    public ResponseEntity<AnswerSubmitResponseDTO> submitAnswer(@AuthenticationPrincipal User user, @PathVariable Long ConversationId,
                                                                @RequestBody AnswerSubmitRequestDTO answerSubmitRequestDTO) {
        return ResponseEntity.ok(answerSubmitService.answerSubmit(user,ConversationId,answerSubmitRequestDTO.getUserAnswer()));
    }
}
