package com.aram3.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public final class TimeMapper {
    private TimeMapper() {}

    public static Instant getInstant(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (Exception e) {
            try {
                String normalized = raw.replace('T', ' ');
                if (normalized.endsWith("Z")) {
                    normalized = normalized.substring(0, normalized.length() - 1);
                }
                int dot = normalized.indexOf('.');
                if (dot > 0) {
                    normalized = normalized.substring(0, dot);
                }
                return java.sql.Timestamp.valueOf(normalized).toInstant();
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}

