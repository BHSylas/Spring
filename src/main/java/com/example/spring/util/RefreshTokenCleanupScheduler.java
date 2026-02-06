package com.example.spring.util;

import com.example.spring.repository.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 만료된 refresh 토큰 정리 배치.
 */
@Component
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupScheduler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /** 매일 새벽 3시(Asia/Seoul)에 만료 토큰 삭제 */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupExpired() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
