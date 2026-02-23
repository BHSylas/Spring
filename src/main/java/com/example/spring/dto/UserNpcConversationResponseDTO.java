package com.example.spring.dto;

import com.example.spring.entity.Country;
import com.example.spring.entity.Level;
import com.example.spring.entity.Place;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserNpcConversationResponseDTO {

    // 대화 기본 정보
    private Long conversationId;
    private Country country;
    private Place place;
    private Level level;
    private String topic;

    private String npcScript;
    private String question;
    private List<String> options;  // BEGINNER만 노출, 나머지 null

    // 진행 상태
    private boolean attempted;    // 한 번이라도 시도했는지
    private boolean correct;      // 정답 여부
    private boolean locked;       // 잠금 여부 (정답 or 3번 실패)
    private int attempts;         // 시도 횟수

    // 잠금 상태일 때만 노출
    private String explanation;
    private List<String> correctAnswer;

    // 다음 대화 연결
    private Long nextConversationId;
}
