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
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String authority = UserRole.fromCode(u.getUserRole()).getAuthority();

        return new org.springframework.security.core.userdetails.User(
                u.getUserEmail(),
                u.getUserPw(),
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}

