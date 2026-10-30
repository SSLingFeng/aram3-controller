package com.aram3.domain;

import java.time.Instant;

public record SysUser(
        Long id,
        String username,
        String passwordHash,
        String displayName,
        String email,
        int status,
        boolean isSuperAdmin,
        int loginFailCount,
        boolean locked,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {}
