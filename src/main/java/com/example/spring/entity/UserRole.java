package com.example.spring.entity;

import lombok.Getter;

@Getter
public enum UserRole {
    USER((byte)0, "ROLE_USER"),
    PROFESSOR((byte)1, "ROLE_PROFESSOR"),
    ADMIN((byte)2, "ROLE_ADMIN");

    private final byte code;
    private final String authority;

    UserRole(byte code, String authority) {
        this.code = code;
        this.authority = authority;
    }

    public static UserRole fromCode(byte code) {
        return switch (code) {
            case 0 -> USER;
            case 1 -> PROFESSOR;
            case 2 -> ADMIN;
            default -> throw new IllegalArgumentException("Invalid role code: " + code);
        };
    }
}
