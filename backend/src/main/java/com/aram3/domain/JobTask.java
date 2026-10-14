package com.aram3.domain;

import java.time.Instant;

public record JobTask(
        Long id,
        String jobType,
        String targetType,
        Long targetId,
        String payloadJson,
        String status,
        int progress,
        String logPath,
        String errorMessage,
        Long createdBy,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt
) {}
