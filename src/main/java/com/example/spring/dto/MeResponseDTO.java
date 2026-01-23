package com.example.spring.dto;

import com.example.spring.entity.User;
import com.example.spring.entity.UserRole;
import lombok.Getter;

@Getter
public class MeResponseDTO {
    private Long userId;
    private String email;
    private String name;
    private String nickname;
    private byte role;
    private String roleName;
    private String createdAt;

    public MeResponseDTO(User u) {
        this.userId = u.getUserId();
        this.email = u.getUserEmail();
        this.name = u.getUserName();
        this.nickname = u.getUserNickname();
        this.role = u.getUserRole();
        this.roleName = UserRole.fromCode(u.getUserRole()).name();
        this.createdAt = u.getCreatedAt().toString();
    }
}
