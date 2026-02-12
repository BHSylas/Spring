package com.example.spring.controller;

import com.example.spring.dto.NpcStatsDTO;
import com.example.spring.entity.User;
import com.example.spring.service.ProfessorStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize("hasRole('PROFESSOR')")
@RestController
@RequestMapping("/api/professor")
@RequiredArgsConstructor
public class ProfessorStatsController {
    private final ProfessorStatsService professorStatsService;

    @GetMapping("/stats")
    public ResponseEntity<List<NpcStatsDTO>> getNpcStats(@AuthenticationPrincipal Long professorId) {
        return ResponseEntity.ok(professorStatsService.getProfessorStats(professorId));
    }

}
