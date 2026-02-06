package com.example.spring.controller;

import com.example.spring.dto.NPCConversationRequestDTO;
import com.example.spring.dto.NPCConversationResponseDTO;
import com.example.spring.entity.Country;
import com.example.spring.entity.Level;
import com.example.spring.entity.Place;
import com.example.spring.service.NpcConversationService;
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
@RequestMapping("/api/professor/npc")
@RequiredArgsConstructor
public class NPCConversationController {
    private final NpcConversationService npcConversationService;

    @PostMapping
    public ResponseEntity<Long> create(@AuthenticationPrincipal Long professorId, @RequestBody NPCConversationRequestDTO requestDTO){
       return ResponseEntity.ok(npcConversationService.create(professorId, requestDTO));

    }

    @GetMapping("/next-candidates/{id}")
    public ResponseEntity<List<NPCConversationResponseDTO>> nextCandidate(@PathVariable Long id,
                                                                          @AuthenticationPrincipal Long professorId){
        return ResponseEntity.ok(npcConversationService.getNextCandidate(professorId, id));
    }

    @PutMapping("/next/{id}")
    public ResponseEntity<Void> nextConversation(@AuthenticationPrincipal Long professorId,
                                                 @PathVariable Long id,
                                                 @RequestParam(required = false) Long nextConversationId){
        npcConversationService.connectNextConversation(professorId, id, nextConversationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<Page<NPCConversationResponseDTO>> list(@AuthenticationPrincipal Long professorId,
                                                                 @RequestParam(required = false)Country country,
                                                                 @RequestParam(required = false) Place place,
                                                                 @RequestParam(required = false) Level level,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size){

        Pageable pageable = PageRequest.of(page, size, Sort.by("npcId").descending());
        return ResponseEntity.ok(npcConversationService.list(professorId,country,place, level,pageable));
    }

    @GetMapping("/list/{id}")
    public ResponseEntity<NPCConversationResponseDTO> detailList(@PathVariable Long id, @AuthenticationPrincipal Long professorId){
        return ResponseEntity.ok(npcConversationService.detailList(id, professorId));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @AuthenticationPrincipal Long professorId,
                                       @RequestBody NPCConversationRequestDTO requestDTO){
        npcConversationService.update(id, professorId, requestDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, @AuthenticationPrincipal Long professorId){
        npcConversationService.deactivate(id, professorId);
        return ResponseEntity.ok().build();
    }




}
