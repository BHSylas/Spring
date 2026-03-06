package com.example.spring.storage;

import com.example.spring.common.exception.BadRequestException;
import com.example.spring.config.AppProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class LocalFileStorage {

    private final Path baseDir;
    private final long maxBytes;
    private final Set<String> allowedExt;

    public LocalFileStorage(AppProperties props) {
        this.baseDir = Paths.get(props.getUpload().getBaseDir()).toAbsolutePath().normalize();
        this.maxBytes = (long) props.getUpload().getMaxFileMb() * 1024 * 1024;
        this.allowedExt = Set.copyOf(props.getUpload().getAllowedExt());
    }

    // =========================================================
    // 영상 저장
    // =========================================================

    public StoredFile saveVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("업로드 파일이 비었습니다.");
        }

        if (file.getSize() > maxBytes) {
            throw new BadRequestException("파일 용량이 너무 큽니다. (최대 " + (maxBytes / 1024 / 1024) + "MB)");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "video" : file.getOriginalFilename());
        String ext = getExtension(original);

        String extNoDot = ext.startsWith(".") ? ext.substring(1) : ext;
        extNoDot = extNoDot.toLowerCase(Locale.ROOT);

        if (StringUtils.hasText(extNoDot) && !allowedExt.contains(extNoDot)) {
            throw new BadRequestException("허용되지 않는 확장자입니다: " + extNoDot);
        }

        String stored = UUID.randomUUID() + (ext.isBlank() ? "" : ext);

        LocalDate now = LocalDate.now();
        Path dir = baseDir.resolve("videos")
                .resolve(String.valueOf(now.getYear()))
                .resolve(String.format("%02d", now.getMonthValue()));

        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(stored);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = baseDir.relativize(target).toString().replace("\\", "/");
            return new StoredFile(
                    "/" + relativePath, // 예: /videos/2026/03/uuid.mp4
                    original,
                    stored,
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException e) {
            throw new BadRequestException("파일 저장 실패: " + e.getMessage());
        }
    }

    // =========================================================
    // 썸네일 저장 (프론트가 만든 이미지 업로드)
    // =========================================================

    public StoredImage saveThumbnail(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null; // 썸네일 선택사항
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("썸네일은 이미지 파일만 업로드할 수 있습니다.");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "thumbnail" : file.getOriginalFilename());
        String ext = getExtension(original);

        String extNoDot = ext.startsWith(".") ? ext.substring(1) : ext;
        extNoDot = extNoDot.toLowerCase(Locale.ROOT);

        if (!Set.of("jpg", "jpeg", "png", "webp").contains(extNoDot)) {
            throw new BadRequestException("허용되지 않는 썸네일 확장자입니다: " + extNoDot);
        }

        String stored = UUID.randomUUID() + (ext.isBlank() ? ".jpg" : ext);

        LocalDate now = LocalDate.now();
        Path dir = baseDir.resolve("thumbnails")
                .resolve(String.valueOf(now.getYear()))
                .resolve(String.format("%02d", now.getMonthValue()));

        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(stored);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = baseDir.relativize(target).toString().replace("\\", "/");
            return new StoredImage(
                    "/" + relativePath, // 예: /thumbnails/2026/03/uuid.jpg
                    original,
                    stored,
                    contentType,
                    file.getSize()
            );
        } catch (IOException e) {
            throw new BadRequestException("썸네일 저장 실패: " + e.getMessage());
        }
    }

    // =========================================================
    // 파일 삭제
    // =========================================================

    public void deleteByLocalPath(String localPath) {
        if (!StringUtils.hasText(localPath)) return;

        String relative = localPath.startsWith("/") ? localPath.substring(1) : localPath;
        Path target = baseDir.resolve(relative).normalize();

        if (!target.startsWith(baseDir)) {
            throw new BadRequestException("잘못된 파일 경로입니다.");
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new BadRequestException("파일 삭제 실패: " + e.getMessage());
        }
    }

    // =========================================================
    // 내부 헬퍼
    // =========================================================

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx < 0) ? "" : filename.substring(idx);
    }

    // =========================================================
    // 반환 DTO
    // =========================================================

    public record StoredFile(
            String localPath,
            String originalFilename,
            String storedFilename,
            String mimeType,
            long fileSizeBytes
    ) {
    }

    public record StoredImage(
            String localPath,
            String originalFilename,
            String storedFilename,
            String mimeType,
            long fileSizeBytes
    ) {
    }
}