package com.aram3.domain;

import java.time.Instant;

public record SteamAccount(
        Long id,
        String username,
        String passwordEnc,
        String guardTokenEnc,
        int isDefault,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
