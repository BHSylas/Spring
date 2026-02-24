package com.example.spring.security;

import com.example.spring.config.AppProperties;
import com.example.spring.common.exception.BadRequestException;
import com.example.spring.common.exception.ForbiddenException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class VideoSignedUrlService {

    private static final String HMAC_ALGO = "HmacSHA256";

    private final String secret;
    private final long ttlSeconds;

    public VideoSignedUrlService(AppProperties props) {
        this.secret = props.getSignedUrl().getSecret();
        this.ttlSeconds = props.getSignedUrl().getTtlSeconds();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.signed-url.secret 값이 비어있습니다.");
        }
    }

    public long computeExpiresEpochSeconds() {
        return Instant.now().getEpochSecond() + ttlSeconds;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public String createSignature(Long videoId, Long uid, String role, long expires) {
        String payload = payload(videoId, uid, role, expires);
        return hmacBase64Url(payload);
    }

    public void validateOrThrow(Long videoId, Long uid, String role, long expires, String sig) {
        if (videoId == null || uid == null) {
            throw new BadRequestException("signed url 파라미터가 올바르지 않습니다.");
        }
        if (role == null || role.isBlank()) {
            throw new BadRequestException("role 파라미터가 필요합니다.");
        }
        if (sig == null || sig.isBlank()) {
            throw new BadRequestException("sig 파라미터가 필요합니다.");
        }

        long now = Instant.now().getEpochSecond();
        if (now > expires) {
            throw new ForbiddenException("재생 URL이 만료되었습니다.");
        }

        String expected = createSignature(videoId, uid, role, expires);

        // constant-time compare
        boolean same = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                sig.getBytes(StandardCharsets.UTF_8)
        );

        if (!same) {
            throw new ForbiddenException("유효하지 않은 재생 URL 서명입니다.");
        }
    }

    private String payload(Long videoId, Long uid, String role, long expires) {
        return videoId + "." + uid + "." + role + "." + expires;
    }

    private String hmacBase64Url(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Signed URL 서명 생성 실패", e);
        }
    }
}