package com.example.spring.service;

import com.example.spring.common.exception.ConflictException;
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


    // Refresh Token 로테이션: 기존 refresh는 폐기하고 새 access/refresh 발급
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

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 refresh 토큰입니다."));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("만료되었거나 폐기된 refresh 토큰입니다.");
        }

        // 로테이션
        stored.revoke();
        refreshTokenRepository.save(stored);

        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String newAccess = jwtService.generateAccessToken(u.getUserId(), u.getUserEmail(), u.getUserRole());
        String newRefresh = jwtService.generateRefreshToken(u.getUserId());
        saveRefreshToken(u.getUserId(), newRefresh);

        return new TokenPairDTO(newAccess, newRefresh, u.getUserNickname(), u.getUserName());
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
