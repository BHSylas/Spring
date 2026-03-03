package com.example.spring.dto;

import java.time.LocalDateTime;

public record AdminUserDashboardDTO(
        long totalUsers,
        long normalUsers,
        long professors,
        long admins,
        long activeUsers,
        long blockedUsers,
        long todaySignups,
        LocalDateTime generatedAt
) {
}