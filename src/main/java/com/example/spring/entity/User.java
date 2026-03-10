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

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 20)
    private UserStatus userStatus;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private User(String userEmail, String userPw, String userName, String userNickname, byte userRole, UserStatus userStatus) {
        this.userEmail = userEmail;
        this.userPw = userPw;
        this.userName = userName;
        this.userNickname = userNickname;
        this.userRole = userRole;
        this.userStatus = (userStatus == null) ? UserStatus.PENDING : userStatus;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.userStatus == null) this.userStatus = UserStatus.PENDING;
    }

    public void changeNickname(String newNickname) {
        this.userNickname = newNickname;
    }

    public void changeRole(byte newRole) {
        this.userRole = newRole;
    }

    public void changeStatus(UserStatus newStatus) {
        this.userStatus = newStatus;
    }

    public void verifyEmail() {
        this.userStatus = UserStatus.ACTIVE;
        this.emailVerifiedAt = LocalDateTime.now();
    }

    public void changeEmail(String newEmail) {
        this.userEmail = newEmail;
    }

    public void withdraw() {
        this.userStatus = UserStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
    }

    public boolean isBlocked() {
        return this.userStatus == UserStatus.BLOCKED;
    }

    public boolean isPending() {
        return this.userStatus == UserStatus.PENDING;
    }

    public boolean isWithdrawn() {
        return this.userStatus == UserStatus.WITHDRAWN;
    }
}
