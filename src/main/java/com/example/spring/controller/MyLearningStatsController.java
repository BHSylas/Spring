package com.example.spring.controller;

import com.example.spring.dto.MyLearningsStatsResponseDTO;
import com.example.spring.service.MyLearningStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/myPage")
@RequiredArgsConstructor
public class MyLearningStatsController {
    private final MyLearningStatsService myLearningStatsService;

    @GetMapping("/learning-stats")
    public ResponseEntity<MyLearningsStatsResponseDTO> getMyLearningsStats(@AuthenticationPrincipal Long userId){
        return ResponseEntity.ok(myLearningStatsService.getMyLearningsStats(userId));
    }
}
