package com.example.spring.dto;

import com.example.spring.entity.Country;
import com.example.spring.entity.Level;
import com.example.spring.entity.Place;
import com.example.spring.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NPCConversationResponseDTO {
    private Long id;
    private Long professorId;

    private Long lectureId;
    private String lectureTitle;

    private Country country;
    private Place place;
    private Level level;

    private String npcScript;
    private String question;

    private List<String> answers;
    private List<String> options;

    private String explanation;
    private String topic;

    private Boolean active;
    private Long nextConversationId;

}
