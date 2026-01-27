package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NPCConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Country country;

    @Enumerated(EnumType.STRING)
    private Place place;

    @Enumerated(EnumType.STRING)
    private Level level;

    private Integer sequence;

    @Column(length = 500)
    private String npcSentence;

    // 초급: 선택지 / 중급: 단어 리스트
    @ElementCollection
    private List<String> options;

    @ElementCollection
    private List<String> answers;

    @Column(length = 500)
    private String explanation;

    private Long nextConversationId;

}
