package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_info",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_email", columnNames = "user_email"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", nullable = false, length = 120)
    private String userEmail;

    @Column(name = "user_pw", nullable = false, length = 255)
    private String userPw; // Hash 저장

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Column(name = "user_nickname", nullable = false, length = 50)
    private String userNickname;

    @Column(name = "user_role", nullable = false)
    private byte userRole; // 0/1/2 (default 0)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private User(String userEmail, String userPw, String userName, String userNickname, byte userRole) {
        this.userEmail = userEmail;
        this.userPw = userPw;
        this.userName = userName;
        this.userNickname = userNickname;
        this.userRole = userRole;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    // 닉네임 수정
    public void changeNickname(String newNickname) {
        this.userNickname = newNickname;
    }
}
