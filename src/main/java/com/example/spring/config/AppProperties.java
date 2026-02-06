package com.example.spring.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cookie cookie = new Cookie();
    private final Security security = new Security();

    private final Upload upload = new Upload();
    private final Youtube youtube = new Youtube();

    @Getter @Setter
    public static class Cookie {
        /** HTTPS 환경에서만 쿠키 전송 여부 */
        private boolean secure = false;

        /** SameSite 정책 (Lax/Strict/None) */
        private String sameSite = "Lax";
    }

    @Getter @Setter
    public static class Security {
        /**
         * 쿠키 기반 요청(/api/auth/refresh, /api/auth/logout)의 Origin/Referer 허용 목록
         * - CSRF 완화용(브라우저가 자동으로 쿠키를 보내는 것을 악용하는 시나리오 차단)
         */
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
    }

    @Getter @Setter
    public static class Upload {
        /** 업로드 파일 저장 기본 폴더 (프로젝트 루트 기준 상대경로도 가능) */
        private String baseDir = "uploads";

        /** 업로드 최대 파일 크기(MB) - 발표용 안전 장치 */
        @Min(1) @Max(2000)
        private int maxFileMb = 200;

        /** 허용 확장자 목록 */
        @NotEmpty
        private List<String> allowedExt = new ArrayList<>(List.of("mp4", "webm", "mov"));

        /** 스트리밍 청크 크기(bytes) */
        @Min(64 * 1024)          // 최소 64KB
        @Max(50 * 1024 * 1024)    // 최대 50MB
        private long chunkSizeBytes = 1024 * 1024; // 1MB
    }

    @Getter @Setter
    public static class Youtube {
        /** YouTube Data API Key (없으면 메타 조회 스킵) */
        private String apiKey = "";
    }
}
