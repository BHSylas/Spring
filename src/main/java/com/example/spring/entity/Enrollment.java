package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_lecture", columnNames = {"user_id", "lecture_id"})
        },
        indexes = {
                @Index(name = "idx_enroll_user", columnList = "user_id"),
                @Index(name = "idx_enroll_lecture", columnList = "lecture_id"),
                @Index(name = "idx_enroll_status", columnList = "status")
        }
)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_enroll_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "lecture_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_enroll_lecture")
    )
    private Lecture lecture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.NOT_STARTED;

    // 전체 진도율(0~100)
    @Column(name = "progress_rate", nullable = false)
    private int progressRate;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 마지막 시청 위치(초)
    @Column(name = "last_watched_time", nullable = false)
    private int lastWatchedTime;

    // 영상 길이(초) - 유튜브/업로드 메타에서 받아서 저장(없으면 0)
    @Column(name = "total_duration", nullable = false)
    private int totalDuration;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Enrollment(User user, Lecture lecture) {
        this.user = user;
        this.lecture = lecture;
        this.status = EnrollmentStatus.NOT_STARTED;
        this.progressRate = 0;
        this.lastWatchedTime = 0;
        this.totalDuration = 0;
    }

    public static Enrollment create(User user, Lecture lecture) {
        return Enrollment.builder()
                .user(user)
                .lecture(lecture)
                .build();
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = EnrollmentStatus.NOT_STARTED;
        if (this.progressRate < 0) this.progressRate = 0;
        if (this.lastWatchedTime < 0) this.lastWatchedTime = 0;
        if (this.totalDuration < 0) this.totalDuration = 0;
    }

    public void updateVideoProgress(int progress, int lastWatchedTime, int totalDuration) {
        int newProgress = Math.max(0, Math.min(progress, 100));

        // 진도 감소 방지
        if (newProgress <= this.progressRate) return;

        this.progressRate = newProgress;
        this.lastWatchedTime = Math.max(0, lastWatchedTime);
        this.totalDuration = Math.max(0, totalDuration);
        this.lastAccessedAt = LocalDateTime.now();

        if (this.progressRate > 0) {
            this.status = EnrollmentStatus.IN_PROGRESS;
        }

        // 완료 기준
        if (this.progressRate >= 100) {
            this.status = EnrollmentStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }
}
