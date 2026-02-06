package com.example.spring.service;

import com.example.spring.common.exception.ConflictException;
import com.example.spring.common.exception.RefreshReplayDetectedException;
import com.example.spring.common.exception.UnauthorizedException;
import com.example.spring.entity.*;
import com.example.spring.dto.*;
import com.example.spring.repository.RefreshTokenRepository;
import com.example.spring.repository.UserRepository;
import com.example.spring.security.JwtService;
import com.example.spring.util.TokenHash;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public void signup(SignUpRequestDTO req) {
        if (userRepository.existsByUserEmail(req.getEmail())) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .userEmail(req.getEmail())
                .userPw(passwordEncoder.encode(req.getPassword()))
                .userName(req.getName())
                .userNickname(req.getNickname())
                .userRole((byte) 0) // 기본 일반유저
                .build();

        userRepository.save(user);
    }

    public TokenPairDTO login(LoginRequestDTO req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User u = userRepository.findByUserEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String access = jwtService.generateAccessToken(u.getUserId(), u.getUserEmail(), u.getUserRole());
        String refresh = jwtService.generateRefreshToken(u.getUserId());
        saveRefreshToken(u.getUserId(), refresh);

        return new TokenPairDTO(access, refresh, u.getUserNickname(), u.getUserName());
    }


    /**
     * Refresh Token 로테이션 + 리플레이 탐지
     * - 정상: revokeIfValid == 1 → 새 토큰 발급
     * - 실패(revokeIfValid == 0):
     *   1) DB에 토큰 hash가 존재하고 revoked=true 이면 "재사용(도난) 가능성"으로 판단
     *      → 해당 userId의 모든 활성 refresh 토큰 revoke(전체 기기 로그아웃)
     *   2) DB에 존재하지만 만료/기타 상태면 401
     *   3) DB에 아예 없으면 401 (위조/탈취/이미 청소됨)
     */
    @Transactional
    public TokenPairDTO refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(refreshToken);
        } catch (Exception e) {
            throw new UnauthorizedException("refresh 토큰이 유효하지 않거나 만료되었습니다.");
        }

        String typ = claims.get("typ", String.class);
        if (!"refresh".equals(typ)) {
            throw new UnauthorizedException("refresh 토큰이 아닙니다.");
        }

        Long userId = Long.valueOf(claims.getSubject());
        String hash = TokenHash.sha256Hex(refreshToken);

        LocalDateTime now = LocalDateTime.now();

        // 원자적 revoke 시도 (동시 요청/재사용 방어 1차)
        int updated = refreshTokenRepository.revokeIfValid(hash, now);
        if (updated != 1) {
            // revoke 실패 → 상태를 확인해서 "리플레이 탐지" 처리
            refreshTokenRepository.findByTokenHash(hash).ifPresentOrElse(rt -> {
                // revoked=true 인데 JWT 자체는 유효(파싱 성공) → 재사용(도난) 가능성이 큼
                if (rt.isRevoked()) {
                    refreshTokenRepository.revokeAllActiveByUserId(userId);
                    throw new RefreshReplayDetectedException("refresh 토큰 재사용이 감지되어 모든 세션에서 로그아웃 처리했습니다.");
                }
                // revoked=false지만 만료/기타 조건 불일치
                throw new UnauthorizedException("만료되었거나 사용할 수 없는 refresh 토큰입니다.");
            }, () -> {
                // DB에 없으면 (이미 정리됨/위조 토큰/탈취 후 삭제 등)
                throw new UnauthorizedException("등록되지 않은 refresh 토큰입니다.");
            });
        }

        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String newAccess = jwtService.generateAccessToken(u.getUserId(), u.getUserEmail(), u.getUserRole());
        String newRefresh = jwtService.generateRefreshToken(u.getUserId());
        saveRefreshToken(u.getUserId(), newRefresh);

        return new TokenPairDTO(newAccess, newRefresh, u.getUserNickname(), u.getUserName());
    }

    /**
     * 로그아웃: refresh 쿠키 값을 기준으로 DB의 refresh 토큰도 폐기한다.
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;

        String hash = TokenHash.sha256Hex(refreshToken);
        refreshTokenRepository.revokeIfActive(hash);
    }

    public MeResponseDTO me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new UnauthorizedException("인증 정보가 없습니다.");
        }

        Long userId = (Long) auth.getPrincipal();

        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return new MeResponseDTO(u);
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        var exp = jwtService.parseClaims(refreshToken).getExpiration().toInstant();
        LocalDateTime expiresAt = LocalDateTime.ofInstant(exp, java.time.ZoneId.of("Asia/Seoul"));

        RefreshToken rt = RefreshToken.builder()
                .userId(userId)
                .tokenHash(TokenHash.sha256Hex(refreshToken))
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(rt);
    }
}
