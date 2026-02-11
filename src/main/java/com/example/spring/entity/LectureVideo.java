package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "lecture_videos",
        indexes = {
                @Index(name = "idx_lecture_videos_lecture", columnList = "lecture_id"),
                @Index(name = "idx_lecture_videos_type", columnList = "source_type")
        },
        uniqueConstraints = {
                // 강의당 영상 1개만 허용하려면 유지
                @UniqueConstraint(name = "uk_lecture_video_lecture", columnNames = {"lecture_id"})
        }
)
public class LectureVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id")
    private Long videoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "lecture_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_video_lecture")
    )
    private Lecture lecture;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private VideoSourceType sourceType;

    // 공통 메타
    @Column(name = "duration_sec", nullable = false)
    private int durationSec; // 없으면 0

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    // ===== 로컬 업로드용 =====
    @Column(name = "local_path", length = 500)
    private String localPath; // 예: /uploads/videos/2026/01/uuid.mp4

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", length = 255)
    private String storedFilename;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    // ===== 유튜브용 =====
    @Column(name = "youtube_video_id", length = 40)
    private String youtubeVideoId;

    @Column(name = "youtube_url", length = 300)
    private String youtubeUrl;

    @Column(name = "youtube_video_title", length = 300)
    private String youtubeVideoTitle;

    @Column(name = "youtube_channel_title", length = 200)
    private String youtubeChannelTitle;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 로컬 업로드용 생성
    public static LectureVideo ofUpload(
            Lecture lecture,
            String localPath,
            String originalFilename,
            String storedFilename,
            String mimeType,
            Long fileSizeBytes,
            int durationSec,
            String thumbnailUrl
    ) {
        LectureVideo v = new LectureVideo();
        v.lecture = lecture;
        v.sourceType = VideoSourceType.UPLOAD;
        v.localPath = localPath;
        v.originalFilename = originalFilename;
        v.storedFilename = storedFilename;
        v.mimeType = mimeType;
        v.fileSizeBytes = fileSizeBytes;
        v.durationSec = Math.max(0, durationSec);
        v.thumbnailUrl = thumbnailUrl;
        return v;
    }

    // 유튜브용 생성
    public static LectureVideo ofYoutube(
            Lecture lecture,
            String youtubeVideoId,
            String youtubeUrl,
            String youtubeVideoTitle,
            String youtubeChannelTitle,
            int durationSec,
            String thumbnailUrl
    ) {
        LectureVideo v = new LectureVideo();
        v.lecture = lecture;
        v.sourceType = VideoSourceType.YOUTUBE;
        v.youtubeVideoId = youtubeVideoId;
        v.youtubeUrl = youtubeUrl;
        v.youtubeVideoTitle = youtubeVideoTitle;
        v.youtubeChannelTitle = youtubeChannelTitle;
        v.durationSec = Math.max(0, durationSec);
        v.thumbnailUrl = thumbnailUrl;
        return v;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    // 발표용 유틸: duration 업데이트 등
    public void updateDuration(int durationSec) {
        this.durationSec = Math.max(0, durationSec);
    }

    public void updateThumbnail(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void updateYoutubeMeta(String videoTitle, String channelTitle, int durationSec, String thumbnailUrl) {
        // 값이 없으면 기존값 유지(안정형)
        if (videoTitle != null && !videoTitle.isBlank()) {
            this.youtubeVideoTitle = videoTitle;
        }
        if (channelTitle != null && !channelTitle.isBlank()) {
            this.youtubeChannelTitle = channelTitle;
        }
        if (durationSec > 0) {
            this.durationSec = durationSec;
        }
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            this.thumbnailUrl = thumbnailUrl;
        }
    }
}
