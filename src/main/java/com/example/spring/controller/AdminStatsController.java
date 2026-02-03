package com.example.spring.controller;

import com.example.spring.dto.NpcStatsDTO;
import com.example.spring.entity.User;
import com.example.spring.service.AdminStatsService;
import com.example.spring.service.ProfessorStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStatsController {
    private final AdminStatsService adminStatsService;

    @GetMapping("/stats")
    public ResponseEntity<List<NpcStatsDTO>> getNpcStats() {
        return ResponseEntity.ok(adminStatsService.getNpcStats());
    }

}
