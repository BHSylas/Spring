package com.example.spring.controller;

import com.example.spring.dto.LectureListItemDTO;
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
     * 학생 공개 강의 목록/검색 (기존 유지: 상세 DTO)
     * - language=ALL이면 전체
     * - enrolling=true/false면 내 수강중/미수강 필터 (로그인 필요)
     * - keyword는 제목/설명 검색
     */
    @GetMapping
    public Page<LectureResponseDTO> listApproved(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") String language,
            @RequestParam(required = false) Boolean enrolling,
            @RequestParam(required = false) String keyword
    ) {
        Long userId = nullableUserId(authentication);
        return lectureService.listApprovedWithFilters(userId, language, enrolling, keyword, pageable(page, size));
    }

    /**
     * 카드용 목록 (썸네일/영상 메타 포함)
     * - 모든 이용자(비로그인 포함)에게 목록 UI로 권장
     * - enrolling 필터는 로그인 필요(서비스에서 검증)
     */
    @GetMapping("/cards")
    public Page<LectureListItemDTO> listApprovedCards(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") String language,
            @RequestParam(required = false) Boolean enrolling,
            @RequestParam(required = false) String keyword
    ) {
        Long userId = nullableUserId(authentication);
        return lectureService.listApprovedLectureCardItems(userId, language, enrolling, keyword, pageable(page, size));
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

    // =========================================================
    // 공통 헬퍼
    // =========================================================

    private Long nullableUserId(Authentication authentication) {
        return (authentication == null) ? null : CurrentUser.getUserId(authentication);
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lectureId"));
    }
}