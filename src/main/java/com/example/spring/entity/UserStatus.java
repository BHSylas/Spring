package com.example.spring.entity;

public enum UserStatus {
    PENDING,   // 이메일 인증 대기
    ACTIVE,    // 인증 완료
    BLOCKED,   // 관리자 차단
    WITHDRAWN  // 회원 탈퇴
}