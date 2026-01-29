package com.example.spring.dto;

import com.example.spring.entity.Country;
import com.example.spring.entity.Level;
import com.example.spring.entity.Place;
import lombok.Getter;

import java.util.List;

@Getter
public class NPCConversationRequestDTO {

    private Long lectureId;
    private Country country;
    private Place place;
    private Level level;

    private String npcScript;
    private String question;

    private List<String> answers;
    private List<String> options;

    private String explanation;
    private String topic;

    private Long nextConversationId;

}
