package com.example.spring.repository;

import com.example.spring.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByExpiresAtBefore(LocalDateTime time);

    /**
     * 원자적으로 refresh 토큰을 폐기(revoked=true)한다.
     * - revoked=false && expiresAt > now 인 경우에만 1이 리턴됨
     * - 동시 refresh(레이스 컨디션) 방어용
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RefreshToken rt
           set rt.revoked = true
         where rt.tokenHash = :hash
           and rt.revoked = false
           and rt.expiresAt > :now
        """)
    int revokeIfValid(@Param("hash") String hash, @Param("now") LocalDateTime now);

    /**
     * 로그아웃 등에서 만료 여부와 무관하게 폐기하고 싶을 때 사용.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RefreshToken rt
           set rt.revoked = true
         where rt.tokenHash = :hash
           and rt.revoked = false
        """)
    int revokeIfActive(@Param("hash") String hash);

    /** 리플레이 탐지 시: 해당 사용자 모든 활성 refresh 토큰 폐기 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RefreshToken rt
           set rt.revoked = true
         where rt.userId = :userId
           and rt.revoked = false
        """)
    int revokeAllActiveByUserId(@Param("userId") Long userId);
}
