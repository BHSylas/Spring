package com.example.spring.security;

import com.example.spring.entity.User;
import com.example.spring.entity.UserRole;
import com.example.spring.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);

        try {
            Claims claims = jwtService.parseClaims(token);

            if (!"access".equals(claims.get("typ", String.class))) {
                chain.doFilter(request, response);
                return;
            }

            Long userId = Long.valueOf(claims.getSubject());

            // DB에서 사용자 상태 확인
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.isBlocked()) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            Integer roleInt = claims.get("role", Integer.class);
            if (roleInt == null) {
                chain.doFilter(request, response);
                return;
            }

            byte roleCode = roleInt.byteValue();
            String authority = UserRole.fromCode(roleCode).getAuthority();

            var authToken = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority(authority))
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}