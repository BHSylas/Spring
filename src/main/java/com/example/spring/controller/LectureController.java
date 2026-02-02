package com.example.spring.controller;

import com.example.spring.dto.LectureResponseDTO;
import com.example.spring.dto.LectureVideoResponseDTO;
import com.example.spring.security.CurrentUser;
import com.example.spring.service.LectureService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lectures")
public class LectureController {

    private final LectureService lectureService;

    /**
     * 강의 목록(승인된 강의)
     * - language=ALL이면 전체
     * - enrolling=true면 내 수강중 강의만
     * - enrolling=false면 미수강 강의만
     */
    @GetMapping
    public Page<LectureResponseDTO> listApproved(Authentication authentication,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(defaultValue = "ALL") String language,
                                                 @RequestParam(required = false) Boolean enrolling) {
        Long userId = CurrentUser.getUserId(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lectureId"));
        return lectureService.listApprovedWithFilters(userId, language, enrolling, pageable);
    }

    /** 강의 상세 */
    @GetMapping("/{lectureId}")
    public LectureResponseDTO getDetail(@PathVariable Long lectureId) {
        return lectureService.getLectureDetail(lectureId);
    }

    /** 강의 영상 정보(유튜브/업로드) */
    @GetMapping("/{lectureId}/video")
    public Optional<LectureVideoResponseDTO> getVideo(@PathVariable Long lectureId) {
        return lectureService.getLectureVideo(lectureId);
    }
}
