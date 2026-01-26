package com.example.spring.Controller;

import com.example.spring.DTO.VideoProgressRequestDTO;
import com.example.spring.DTO.VideoProgressResponseDTO;
import com.example.spring.Service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lecture")
public class ProgressController {
    private final ProgressService progressService;

    @PostMapping("/progress")
    public VideoProgressResponseDTO saveVideoProgress(@RequestBody VideoProgressRequestDTO request,
                                                      @AuthenticationPrincipal User user) {
        return progressService.updateVideoProgress(
                user,
                request.getLectureId(),
                request.getCurrentTime(),
                request.getDuration()
        );
    }
}
