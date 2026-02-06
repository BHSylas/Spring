package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "lectures",
        indexes = {
                @Index(name = "idx_lectures_professor", columnList = "professor_id"),
                @Index(name = "idx_lectures_status", columnList = "status"),
                @Index(name = "idx_lectures_status_lang", columnList = "status, language"),
                @Index(name = "idx_lectures_approved_by", columnList = "approved_by")
        }
)
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lecture_id")
    private Long lectureId;

    @Column(nullable = false, length = 120)
    private String title;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 60)
    private String country;

    @Column(nullable = false, length = 60)
    private String language;

    // 강사(User)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "professor_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_lecture_professor")
    )
    private User professor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LectureStatus status = LectureStatus.PENDING;

    // ===== 관리자 승인 정보 =====
    // 승인/반려 처리한 관리자(User)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "approved_by",
            foreignKey = @ForeignKey(name = "fk_lecture_approved_by")
    )
    private User approvedBy;

    // 승인/반려 처리 시각
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // 반려 사유
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    // 생성/수정 시간
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    private Lecture(String title, String description, String country, String language, User professor) {
        this.title = title;
        this.description = description;
        this.country = country;
        this.language = language;
        this.professor = professor;
        this.status = LectureStatus.PENDING;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = LectureStatus.PENDING;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 강의 기본 정보 수정 (승인 상태는 변경하지 않음)
    public void updateInfo(String title, String description, String country, String language) {
        this.title = title;
        this.description = description;
        this.country = country;
        this.language = language;
    }

    // ===== 관리자 액션 =====
    public void approve(User admin) {
        this.status = LectureStatus.APPROVED;
        this.approvedBy = admin;
        this.approvedAt = LocalDateTime.now();
        this.rejectReason = null; // 승인 시 반려사유 제거
    }

    public void reject(User admin, String reason) {
        this.status = LectureStatus.REJECTED;
        this.approvedBy = admin;
        this.approvedAt = LocalDateTime.now();
        this.rejectReason = reason;
    }

    // 다시 승인대기로 돌리기 — 운영 편의용
    public void resetToPending() {
        this.status = LectureStatus.PENDING;
        this.approvedBy = null;
        this.approvedAt = null;
        this.rejectReason = null;
    }
}
