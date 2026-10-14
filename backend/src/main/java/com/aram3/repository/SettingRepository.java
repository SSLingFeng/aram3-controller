package com.aram3.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class SettingRepository {
    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> get(String key) {
        return jdbc.query("SELECT value FROM sys_setting WHERE key = ?",
                rs -> rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty(), key);
    }

    public Map<String, String> all() {
        Map<String, String> map = new LinkedHashMap<>();
        jdbc.query("SELECT key, value FROM sys_setting ORDER BY key", rs -> {
            map.put(rs.getString("key"), rs.getString("value"));
        });
        return map;
    }

    public void upsert(String key, String value, String description) {
        String now = Instant.now().toString();
        int updated = jdbc.update("UPDATE sys_setting SET value=?, description=?, updated_at=? WHERE key=?",
                value, description, now, key);
        if (updated == 0) {
            jdbc.update("INSERT INTO sys_setting(key, value, description, updated_at) VALUES (?,?,?,?)",
                    key, value, description, now);
        }
    }
}
