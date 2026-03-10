package com.example.spring.security;

import com.example.spring.entity.User;
import com.example.spring.entity.UserRole;
import com.example.spring.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        if (u.isWithdrawn()) {
            throw new UsernameNotFoundException("탈퇴한 계정입니다.");
        }

        if (u.isBlocked()) {
            throw new UsernameNotFoundException("차단된 계정입니다.");
        }

        if (u.isPending()) {
            throw new UsernameNotFoundException("이메일 인증이 필요한 계정입니다.");
        }

        String authority = UserRole.fromCode(u.getUserRole()).getAuthority();

        return new org.springframework.security.core.userdetails.User(
                u.getUserEmail(),
                u.getUserPw(),
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}