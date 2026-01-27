package com.example.spring.controller;

import com.example.spring.dto.LectureListResponseDTO;
import com.example.spring.entity.User;
import com.example.spring.service.LectureListService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lecture")
public class LectureListController {
    private final LectureListService lectureListService;

    @GetMapping("/list")
    public Page<LectureListResponseDTO> getLectureList(@RequestParam(defaultValue = "ALL") String language,
                                                       @RequestParam(defaultValue = "ALL")String enrolling,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "5") int size,
                                                       @AuthenticationPrincipal User user) {
        return lectureListService.getLectureList(
                user,
                language,
                enrolling,
                page - 1,
                size
        );
    }
}
