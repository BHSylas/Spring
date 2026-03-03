package com.example.spring.repository;

import com.example.spring.entity.User;
import com.example.spring.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserEmail(String userEmail);
    boolean existsByUserEmail(String userEmail);

    // 관리자 검색
    Page<User> findByUserEmailContainingIgnoreCaseOrUserNicknameContainingIgnoreCase(
            String emailKeyword,
            String nicknameKeyword,
            Pageable pageable
    );

    // 대시보드 집계
    long countByUserRole(byte userRole);
    long countByCreatedAtGreaterThanEqual(LocalDateTime since);
    long countByUserStatus(UserStatus userStatus);
}
