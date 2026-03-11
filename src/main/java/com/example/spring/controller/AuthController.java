package com.example.spring.controller;

import com.example.spring.common.exception.UnauthorizedException;
import com.example.spring.dto.*;
import com.example.spring.security.CurrentUser;
import com.example.spring.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequestDTO req) {
        authService.sendVerificationCode(req.getEmail());
        return ResponseEntity.ok("인증번호를 이메일로 발송했습니다.");
    }

    @PostMapping("/verify-email-code")
    public ResponseEntity<?> verifyEmailCode(@Valid @RequestBody VerifyEmailCodeRequestDTO req) {
        authService.verifyEmailCode(req.getEmail(), req.getCode());
        return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignUpRequestDTO req) {
        authService.signup(req);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    // 로그인: refreshToken은 HttpOnly 쿠키로, 바디는 accessToken + 유저정보
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO req,
            HttpServletResponse res
    ) {
        // AuthService.login이 TokenPairDTO를 리턴하도록 바꿔야 함
        TokenPairDTO tokens = authService.login(req);

        setRefreshCookie(res, tokens.getRefreshToken());

        return ResponseEntity.ok(
                new AuthResponseDTO(tokens.getAccessToken(), tokens.getUserNickname(), tokens.getUserName())
        );
    }

    // refresh: 요청 바디 제거하고 쿠키에서 refreshToken을 읽음
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse res
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("refresh 토큰이 없습니다.");
        }

        try {
            TokenPairDTO tokens = authService.refresh(refreshToken);
            setRefreshCookie(res, tokens.getRefreshToken());

            return ResponseEntity.ok(
                    new AuthResponseDTO(tokens.getAccessToken(), tokens.getUserNickname(), tokens.getUserName())
            );
        } catch (com.example.spring.common.exception.RefreshReplayDetectedException e) {
            // 리플레이 감지 → 쿠키도 지워서 클라이언트 상태를 깔끔히 정리
            clearRefreshCookie(res);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name="refreshToken", required=false) String refreshToken,
            HttpServletResponse res
    ){
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken); // DB revoke
        }
        clearRefreshCookie(res);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            Authentication authentication,
            HttpServletResponse res
    ) {
        System.out.println(">>> AuthController withdraw 진입");
        Long currentUserId = CurrentUser.getUserId(authentication);
        authService.withdraw(currentUserId);
        clearRefreshCookie(res);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponseDTO> me() {
        return ResponseEntity.ok(authService.me());
    }

    @PatchMapping("/me/profile")
    public MeResponseDTO updateMyProfile(@RequestBody @Valid UpdateMyProfileRequestDTO req,
                                         Authentication authentication) {
        Long currentUserId = CurrentUser.getUserId(authentication);
        return authService.updateMyProfile(currentUserId, req);
    }

    @PostMapping("/find-email")
    public Map<String, String> findEmail(@RequestBody @Valid FindEmailRequestDTO req) {
        authService.findEmail(req);
        return Map.of("message", "입력하신 정보와 일치하는 계정이 있으면 안내 메일을 발송했습니다.");
    }

    @PostMapping("/password-reset/send-code")
    public Map<String, String> sendPasswordResetCode(@RequestBody @Valid PasswordResetSendCodeRequestDTO req) {
        authService.sendPasswordResetCode(req);
        return Map.of("message", "인증번호가 발송되었습니다.");
    }

    @PostMapping("/password-reset/verify-code")
    public Map<String, String> verifyPasswordResetCode(@RequestBody @Valid PasswordResetVerifyCodeRequestDTO req) {
        authService.verifyPasswordResetCode(req);
        return Map.of("message", "인증이 완료되었습니다.");
    }

    @PostMapping("/password-reset/confirm")
    public Map<String, String> resetPassword(@RequestBody @Valid PasswordResetConfirmRequestDTO req) {
        authService.resetPassword(req);
        return Map.of("message", "비밀번호가 변경되었습니다.");
    }

    // ===== cookie helpers =====
    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site}")
    private String cookieSameSite;

    @Value("${jwt.refresh-token-exp-days}")
    private int refreshTokenExpDays;

    private void setRefreshCookie(HttpServletResponse res, String refreshToken) {
        long maxAgeSeconds = (long) refreshTokenExpDays * 24 * 60 * 60;

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(cookieSameSite)
                .build();

        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }


    private void clearRefreshCookie(HttpServletResponse res) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();

        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
