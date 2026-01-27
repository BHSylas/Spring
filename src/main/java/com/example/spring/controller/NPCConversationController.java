package com.example.spring.controller;

import com.example.spring.dto.ConversationResponseDTO;
import com.example.spring.entity.User;
import com.example.spring.service.NpcConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/npc")
@RequiredArgsConstructor
public class NPCConversationController {
    private final NpcConversationService npcConversationService;

    @PostMapping("/{conversationId}/answer")
    public ResponseEntity<ConversationResponseDTO> answer(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user,
            @RequestParam String answer
    ) {
        return ResponseEntity.ok(
                npcConversationService.submitAnswer(user, conversationId, answer)
        );
    }

}
