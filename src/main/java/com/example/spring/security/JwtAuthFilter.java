package com.example.spring.security;

import com.example.spring.entity.User;
import com.example.spring.entity.UserRole;
import com.example.spring.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims = jwtService.parseClaims(token);

            String typ = claims.get("typ", String.class);
            if (!"access".equals(typ)) {
                chain.doFilter(request, response);
                return;
            }

            Long userId = Long.valueOf(claims.getSubject());

            User user = userRepository.findById(userId).orElse(null);

            if (user == null || user.isBlocked() || user.isWithdrawn() || user.isPending()) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            String authority = null;
            Object roleClaim = claims.get("role");

            if (roleClaim instanceof Number number) {
                byte roleCode = number.byteValue();
                authority = UserRole.fromCode(roleCode).getAuthority();
            } else if (roleClaim instanceof String roleString) {
                if (roleString.startsWith("ROLE_")) {
                    authority = roleString;
                } else {
                    authority = "ROLE_" + roleString;
                }
            }

            if (authority == null || authority.isBlank()) {
                String auth = claims.get("auth", String.class);
                if (auth != null && !auth.isBlank()) {
                    authority = auth;
                }
            }

            if (authority == null || authority.isBlank()) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority(authority))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}