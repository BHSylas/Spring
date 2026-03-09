package com.example.spring.dto;

import com.example.spring.entity.ProfessorProfile;
import lombok.Getter;

@Getter
public class ProfessorProfileResponseDTO {

    private final Long professorId;
    private final String professorName;
    private final String professorNickname;
    private final String bio;
    private final String specialty;
    private final String career;
    private final String profileImageUrl;
    private final String office;
    private final String contactEmail;

    public ProfessorProfileResponseDTO(ProfessorProfile profile) {
        this.professorId = profile.getUser().getUserId();
        this.professorName = profile.getUser().getUserName();
        this.professorNickname = profile.getUser().getUserNickname();
        this.bio = profile.getBio();
        this.specialty = profile.getSpecialty();
        this.career = profile.getCareer();
        this.profileImageUrl = profile.getProfileImageUrl();
        this.office = profile.getOffice();
        this.contactEmail = profile.getContactEmail();
    }
}