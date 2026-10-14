package com.aram3.domain;

import java.time.Instant;

public record InstanceMod(
        Long id,
        Long instanceId,
        Long modId,
        String loadType,
        int enabled,
        int loadOrder,
        String customArgs,
        Instant installedAt,
        Instant updatedAt
) {}
