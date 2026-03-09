package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "professor_profiles")
public class ProfessorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_professor_profile_user")
    )
    private User user;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "specialty", length = 200)
    private String specialty;

    @Column(name = "career", length = 1000)
    private String career;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "office", length = 200)
    private String office;

    @Column(name = "contact_email", length = 120)
    private String contactEmail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    private ProfessorProfile(
            User user,
            String bio,
            String specialty,
            String career,
            String profileImageUrl,
            String office,
            String contactEmail
    ) {
        this.user = user;
        this.bio = bio;
        this.specialty = specialty;
        this.career = career;
        this.profileImageUrl = profileImageUrl;
        this.office = office;
        this.contactEmail = contactEmail;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(
            String bio,
            String specialty,
            String career,
            String profileImageUrl,
            String office,
            String contactEmail
    ) {
        this.bio = bio;
        this.specialty = specialty;
        this.career = career;
        this.profileImageUrl = profileImageUrl;
        this.office = office;
        this.contactEmail = contactEmail;
    }
}