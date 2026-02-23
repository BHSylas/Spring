package com.example.spring.dto;

import com.example.spring.entity.Country;
import com.example.spring.entity.Level;
import com.example.spring.entity.Place;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NpcProgressResponseDTO {
    private long totalCount;
    private long solvedCount;
    private long correctCount;
    private double progressRate;
    private double correctRate;

    private List<NpcCountryProgressDTO> countries;

    //국가 단위
    @Getter
    @Builder
    public static class NpcCountryProgressDTO {
        private Country country;

        private long totalCount;
        private long solvedCount;
        private long correctCount;
        private double progressRate;
        private double correctRate;

        private List<NpcLevelProgressDTO> levels;

    }

    //레벨 단위
    @Getter
    @Builder
    public static class NpcLevelProgressDTO {
        private Level level;

        private long totalCount;
        private long solvedCount;
        private long correctCount;
        private double progressRate;
        private double correctRate;

        private List<NpcPlaceProgressDTO> places;
    }

    //장소 단위
    @Getter
    @Builder
    public static class NpcPlaceProgressDTO {
        private Place place;

        private long totalCount;
        private long solvedCount;
        private long correctCount;
        private double progressRate;
        private double correctRate;

        private List<NpcItemStatusDTO> items;
    }

    //문제단위
    @Getter
    @Builder
    public static class NpcItemStatusDTO {
        private Long conversationId;
        private String topic;
        private boolean attempted;
        private boolean correct;
        private boolean locked;
        private int attempts;
        private Long nextConversationId;


    }
}
