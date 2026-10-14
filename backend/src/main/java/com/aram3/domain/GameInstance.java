package com.aram3.domain;

import java.time.Instant;

public record GameInstance(
        Long id,
        String code,
        String name,
        String description,
        String rootPath,
        Integer gamePort,
        Integer steamQueryPort,
        String bindIp,
        String rconHost,
        Integer rconPort,
        String rconPassword,
        Integer maxPlayers,
        String profileName,
        String startScript,
        String extraArgs,
        String status,
        int enabled,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
