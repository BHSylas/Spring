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
    Optional<User> findByUserNameAndUserNicknameAndUserStatus(String userName, String userNickname, UserStatus userStatus);
    boolean existsByUserEmail(String userEmail);

    boolean existsByUserEmailAndUserStatusNot(String userEmail, UserStatus userStatus);

    // 닉네임 중복 제한
    boolean existsByUserNickname(String userNickname);

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
