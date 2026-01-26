package com.example.spring.Entity;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.boot.security.autoconfigure.SecurityProperties;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = { @UniqueConstraint(name = "uk_user_lecture", columnNames = {"user_id", "lecture_id"})})

public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    /* 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    /* 진도 */
    @Column(nullable = false)
    private int progressRate;        // 전체 진도 (0~100)

    private LocalDateTime lastAccessedAt;
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private int totalDuration;

    @Column(nullable = false)
    private int lastWatchedTime; //마지막 시청 위치(초)

    /* 생성 */
    public static Enrollment create(User user, Lecture lecture) {
        return Enrollment.builder()
                .user(user)
                .lecture(lecture)
                .status(EnrollmentStatus.NOT_STARTED)
                .progressRate(0)
                .lastWatchedTime(0)
                .totalDuration(0)
                .build();
    }

    public void updateVideoProgress(
            int progress,
            int lastWatchedTime,
            int totalDuration
    ) {
        int newProgress = Math.min(progress, 100);

        if (newProgress <= this.progressRate) {
            return; // 진도 감소 방지
        }

        this.progressRate = newProgress;
        this.lastWatchedTime = lastWatchedTime;
        this.totalDuration = totalDuration;
        this.lastAccessedAt = LocalDateTime.now();

        if (this.progressRate > 0) {
            this.status = EnrollmentStatus.IN_PROGRESS;
        }

        if (this.progressRate >= 80) {
            this.status = EnrollmentStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }







}
