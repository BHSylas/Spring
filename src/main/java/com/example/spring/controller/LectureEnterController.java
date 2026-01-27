package com.example.spring.controller;

import com.example.spring.dto.LectureEnterRequestDTO;
import com.example.spring.dto.LectureEnterResponseDTO;
import com.example.spring.entity.User;
import com.example.spring.service.LectureEnterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/lecture")
public class LectureEnterController {
    private final LectureEnterService lectureEnterService;

    @GetMapping("/enter")
    public LectureEnterResponseDTO enterLecture(@RequestBody LectureEnterRequestDTO requestDTO
            , @AuthenticationPrincipal User user) {
        return lectureEnterService.enterLecture(user, requestDTO.getLectureId());
    }
}
