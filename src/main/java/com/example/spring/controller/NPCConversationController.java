package com.example.spring.controller;

import com.example.spring.dto.ConversationResponseDTO;
import com.example.spring.dto.NPCConversationRequestDTO;
import com.example.spring.dto.NPCConversationResponseDTO;
import com.example.spring.entity.Country;
import com.example.spring.entity.Level;
import com.example.spring.entity.Place;
import com.example.spring.entity.User;
import com.example.spring.service.NpcConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professor/npc")
@RequiredArgsConstructor
public class NPCConversationController {
    private final NpcConversationService npcConversationService;

    @PostMapping
    public ResponseEntity<Void> create(@AuthenticationPrincipal User professor, @RequestBody NPCConversationRequestDTO requestDTO){
        npcConversationService.create(professor.getUserId(), requestDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/next-candidates/{id}")
    public ResponseEntity<List<NPCConversationResponseDTO>> nextCandidate(@PathVariable Long id,
                                                                          @AuthenticationPrincipal User professor){
        return ResponseEntity.ok(npcConversationService.getNextCandidate(professor.getUserId(), id));
    }

    @PutMapping("/next/{id}")
    public ResponseEntity<Void> nextConversation(@AuthenticationPrincipal User professor,
                                                 @PathVariable Long id,
                                                 @RequestParam(required = false) Long nextConversationId){
        npcConversationService.connectNextConversation(professor.getUserId(), id, nextConversationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<NPCConversationResponseDTO>> list(@AuthenticationPrincipal User professor,
                                                                 @RequestParam(required = false)Country country,
                                                                 @RequestParam(required = false) Place place,
                                                                 @RequestParam(required = false) Level level){
        return ResponseEntity.ok(npcConversationService.list(professor.getUserId(),country,place, level));
    }

    @GetMapping("/list/{id}")
    public ResponseEntity<NPCConversationResponseDTO> detailList(@PathVariable Long id, @AuthenticationPrincipal User professor){
        return ResponseEntity.ok(npcConversationService.detailList(id, professor.getUserId()));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @AuthenticationPrincipal User professor,
                                       @RequestBody NPCConversationRequestDTO requestDTO){
        npcConversationService.update(id, professor.getUserId(), requestDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, @AuthenticationPrincipal User professor){
        npcConversationService.deactivate(id, professor.getUserId());
        return ResponseEntity.ok().build();
    }




}
