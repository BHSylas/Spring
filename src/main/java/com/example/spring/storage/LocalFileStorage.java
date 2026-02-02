package com.example.spring.storage;

import com.example.spring.config.AppProperties;
import com.example.spring.common.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class LocalFileStorage {

    private final Path baseDir;
    private final long maxBytes;
    private final Set<String> allowedExt;

    public LocalFileStorage(AppProperties props) {
        this.baseDir = Paths.get(props.getUpload().getBaseDir()).toAbsolutePath().normalize();
        this.maxBytes = (long) props.getUpload().getMaxFileMb() * 1024 * 1024;

        this.allowedExt = props.getUpload().getAllowedExt().stream()
                .filter(StringUtils::hasText)
                .map(s -> s.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toSet());
    }

    public StoredFile saveVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("업로드 파일이 비었습니다.");

        if (file.getSize() > maxBytes) {
            throw new BadRequestException("파일 용량이 너무 큽니다. (최대 " + (maxBytes / 1024 / 1024) + "MB)");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "video" : file.getOriginalFilename());
        String ext = getExtension(original); // ".mp4" 또는 ""

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
                    "/" + relativePath, // 예: /videos/2026/02/uuid.mp4
                    original,
                    stored,
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException e) {
            throw new BadRequestException("파일 저장 실패: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return "";
        return filename.substring(dot);
    }

    public record StoredFile(
            String localPath,
            String originalFilename,
            String storedFilename,
            String mimeType,
            long fileSizeBytes
    ) {}
}
