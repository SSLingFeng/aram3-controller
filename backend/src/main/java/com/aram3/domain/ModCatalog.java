package com.aram3.domain;

import java.time.Instant;

public record ModCatalog(
        Long id,
        String name,
        String workshopId,
        String author,
        String version,
        String fileName,
        String filePath,
        Long fileSize,
        String checksum,
        String defaultLoadType,
        String downloadStatus,
        String description,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
