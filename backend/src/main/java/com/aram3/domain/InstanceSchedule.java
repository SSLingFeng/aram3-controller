package com.aram3.domain;

import java.time.Instant;

public record InstanceSchedule(
        Long id,
        Long instanceId,
        String action,
        String cronExpr,
        String timezone,
        int enabled,
        Instant nextRunAt,
        Instant lastRunAt,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
