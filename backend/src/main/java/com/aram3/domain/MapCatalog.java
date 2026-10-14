package com.aram3.domain;

import java.time.Instant;

public record MapCatalog(
        Long id,
        String name,
        String mapKey,
        String fileName,
        String filePath,
        Long fileSize,
        String checksum,
        String sourceType,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
