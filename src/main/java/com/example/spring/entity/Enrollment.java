package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_lecture", columnNames = {"user_id", "lecture_id"})
        },
        indexes = {
                @Index(name = "idx_enroll_user_status", columnList = "user_id, status"),
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

    // 취소 시각
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    // 마지막 시청 위치(초)
    @Column(name = "last_watched_time", nullable = false)
    private int lastWatchedTime;

    // 영상 길이(초)
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

    /** 수강 취소(soft cancel) */
    public void cancel() {
        if (this.status == EnrollmentStatus.CANCELED) return;
        this.status = EnrollmentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();

        // 취소면 완료는 무효 처리
        this.completedAt = null;
    }

    // 재수강
    public void reactivateAndReset() {
        this.status = EnrollmentStatus.NOT_STARTED;
        this.progressRate = 0;
        this.lastWatchedTime = 0;
        this.totalDuration = Math.max(0, this.totalDuration);
        this.lastAccessedAt = LocalDateTime.now();
        this.completedAt = null;
        this.canceledAt = null;
    }

    // 진도 업데이트
    public void updateVideoProgress(int progress, int lastWatchedTime, int totalDuration) {
        // 취소된 수강은 업데이트 불가
        if (this.status == EnrollmentStatus.CANCELED) {
            throw new IllegalStateException("취소된 수강은 진도를 업데이트할 수 없습니다.");
        }

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
