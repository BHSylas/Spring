package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NPCConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "npc_id")
    private Long npcId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "lecture_id", nullable = false)
    private Lecture lecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;

    @Enumerated(EnumType.STRING)
    private Country country;

    @Enumerated(EnumType.STRING)
    private Place place;

    @Enumerated(EnumType.STRING)
    private Level level;

    @Column(length = 1000)
    private String npcScript;

    @Column(length = 1000)
    private String question;

    // 초급: 선택지 / 중급: 단어 리스트
    @ElementCollection
    private List<String> options;

    @ElementCollection
    private List<String> answers;

    @Column(length = 500)
    private String explanation;

    private boolean active;

    private String topic;
    private Long nextConversationId;


    public void update(
            String npcScript,
            String question,
            List<String> options,
            List<String> answers,
            String explanation,
            String topic){
        this.npcScript = npcScript;
        this.question = question;
        this.options = options;
        this.answers = answers;
        this.explanation = explanation;
        this.topic = topic;
    }


    public void deactivate(){
        this.active = false;
    }
}
