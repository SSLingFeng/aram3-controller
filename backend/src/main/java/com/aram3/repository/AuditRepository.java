package com.aram3.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public class AuditRepository {
    private final JdbcTemplate jdbc;

    public AuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(Long userId, String action, String targetType, Long targetId, String detailJson, String ip) {
        jdbc.update("""
                INSERT INTO audit_log(user_id, action, target_type, target_id, detail_json, ip, created_at)
                VALUES (?,?,?,?,?,?,?)
                """, userId, action, targetType, targetId, detailJson, ip, Instant.now().toString());
    }

    public List<Map<String, Object>> recent(int limit) {
        return jdbc.queryForList("SELECT * FROM audit_log ORDER BY id DESC LIMIT ?", limit);
    }
}


