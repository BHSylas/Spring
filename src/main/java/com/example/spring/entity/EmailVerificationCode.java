package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "email_verification_codes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_email_verification_email_purpose", columnNames = {"email", "purpose"})
        })
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private VerificationPurpose purpose;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private EmailVerificationCode(String email, VerificationPurpose purpose, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.purpose = purpose;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return this.expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isVerified() {
        return this.verifiedAt != null;
    }

    public boolean matches(String inputCode) {
        return this.code.equals(inputCode);
    }

    public void markVerified() {
        this.verifiedAt = LocalDateTime.now();
    }

    public void renew(String newCode, LocalDateTime newExpiresAt) {
        this.code = newCode;
        this.expiresAt = newExpiresAt;
        this.verifiedAt = null;
    }
}