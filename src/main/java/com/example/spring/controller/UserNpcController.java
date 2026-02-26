package com.example.spring.controller;

import com.example.spring.dto.NpcProgressResponseDTO;
import com.example.spring.dto.UserNpcConversationResponseDTO;
import com.example.spring.entity.Country;
import com.example.spring.entity.Level;
import com.example.spring.entity.Place;
import com.example.spring.service.UserNpcService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@RestController
@RequestMapping("/api/user/npc")
@RequiredArgsConstructor
public class UserNpcController {

    private final UserNpcService userNpcService;

    /**
     * NPC 학습 시작 - 미풀이 첫 번째 문제 반환
     * GET /api/user/npc/start
     *
     * 필터를 모두 생략하면 전체 NPC 중 미풀이 첫 번째를 반환.
     * 메타버스에서 특정 국가/장소/레벨 진입 시 필터를 지정해 호출.
     *
     * @param country 국가 필터 (선택): USA, UK, JAPAN, GERMANY, ITALY, FRANCE, CHINA
     * @param place   장소 필터 (선택): CAPE, RESTAURANT, CONVENIENCE_STORE, SCHOOL ...
     * @param level   난이도 필터 (선택): BEGINNER, INTERMEDIATE, ADVANCED
     */
    @GetMapping("/start")
    public ResponseEntity<UserNpcConversationResponseDTO> start(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Country country,
            @RequestParam(required = false) Place place,
            @RequestParam(required = false) Level level
    ) {
        return ResponseEntity.ok(userNpcService.start(userId, country, place, level));
    }

    /**
     * 전체 NPC 문제 목록 조회 (메타버스 진입 시 한 번에 로드)
     * GET /api/user/npc/list
     *
     * 필터를 모두 생략하면 전체 활성 NPC를 반환.
     * 메타버스에서 특정 국가/장소/레벨 진입 시 필터를 지정해 호출.
     */
    @GetMapping("/list")
    public ResponseEntity<List<UserNpcConversationResponseDTO>> getAllConversations(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Country country,
            @RequestParam(required = false) Place place,
            @RequestParam(required = false) Level level
    ) {
        return ResponseEntity.ok(userNpcService.getAllConversations(userId, country, place, level));
    }

    /**
     * 특정 NPC 대화 단건 조회
     * GET /api/user/npc/conversation/{conversationId}
     *
     * 정답 제출(AnswerSubmitController) 후 nextConversationId로 다음 문제를 가져올 때 사용.
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<UserNpcConversationResponseDTO> getConversation(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long conversationId
    ) {
        return ResponseEntity.ok(userNpcService.getConversation(userId, conversationId));
    }

    /**
     * 전체 NPC 진행 현황 조회 (국가 → 레벨 → 장소 3단계 계층)
     * GET /api/user/npc/progress
     *
     * 필터 없이 호출 시 전체 현황 반환.
     * 특정 국가만 보고 싶다면 ?country=USA 처럼 필터 적용.
     *
     * @param country 국가 필터 (선택)
     * @param level   난이도 필터 (선택)
     * @param place   장소 필터 (선택)
     */
    @GetMapping("/progress")
    public ResponseEntity<NpcProgressResponseDTO> getProgress(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Country country,
            @RequestParam(required = false) Level level,
            @RequestParam(required = false) Place place
    ) {
        return ResponseEntity.ok(userNpcService.getProgress(userId, country, level, place));
    }

}