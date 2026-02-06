package com.example.spring.security;

import com.example.spring.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * 쿠키 기반 엔드포인트(/api/auth/refresh, /api/auth/logout)에 대해 Origin/Referer를 검증해
 * 기본 CSRF 표면을 줄이는 필터.
 *
 * - 브라우저는 타 사이트에서 요청해도 쿠키를 자동으로 첨부할 수 있어 위험
 * - Origin(또는 Referer)의 "출처"가 허용 목록에 없으면 403 반환
 *
 * 참고: API 클라이언트(curl/postman)처럼 Origin/Referer가 아예 없는 경우는 통과시킨다.
 */
@Component
public class OriginCsrfFilter extends OncePerRequestFilter {

    private final List<String> allowedOrigins;

    public OriginCsrfFilter(AppProperties props) {
        this.allowedOrigins = props.getSecurity().getAllowedOrigins();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method)) return true;

        String path = request.getRequestURI();
        return !("/api/auth/refresh".equals(path) || "/api/auth/logout".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 허용 목록이 비어 있으면 검증을 스킵(개발/테스트 환경에서 편의)
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        // Origin이 없으면 Referer에서 출처를 추출
        String requestOrigin = (origin != null && !origin.isBlank())
                ? origin
                : extractOriginFromReferer(referer);

        // 둘 다 없으면(비브라우저 호출 가능성) 통과
        if (requestOrigin == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!isAllowed(requestOrigin)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"CSRF 보호: 허용되지 않은 Origin 입니다.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String requestOrigin) {
        for (String allowed : allowedOrigins) {
            if (allowed == null || allowed.isBlank()) continue;
            if (allowed.equalsIgnoreCase(requestOrigin)) return true;
        }
        return false;
    }

    private String extractOriginFromReferer(String referer) {
        if (referer == null || referer.isBlank()) return null;
        try {
            URI uri = URI.create(referer);
            if (uri.getScheme() == null || uri.getHost() == null) return null;

            int port = uri.getPort();
            String o = uri.getScheme() + "://" + uri.getHost();
            if (port != -1) o += ":" + port;
            return o;
        } catch (Exception e) {
            return null;
        }
    }
}
