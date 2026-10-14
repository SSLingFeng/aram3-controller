package com.aram3.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public class KeyRepository {
    private final JdbcTemplate jdbc;

    public KeyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void ensureSchema() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS key_catalog (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  file_name TEXT NOT NULL,
                  file_path TEXT NOT NULL,
                  checksum TEXT,
                  source_type TEXT NOT NULL DEFAULT 'MOD',
                  source_mod_id INTEGER,
                  source_mod_name TEXT,
                  created_at TEXT NOT NULL,
                  updated_at TEXT NOT NULL
                )
                """);
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_key_name ON key_catalog(file_name)");
    }

    public List<Map<String, Object>> findAll() {
        ensureSchema();
        return jdbc.queryForList("SELECT * FROM key_catalog ORDER BY file_name");
    }

    public int update(String fileName, String filePath, String checksum,
                      String sourceType, Long modId, String modName, String now) {
        ensureSchema();
        return jdbc.update("""
                UPDATE key_catalog SET file_path=?, checksum=?, source_type=?, source_mod_id=?,
                  source_mod_name=?, updated_at=? WHERE file_name=?
                """, filePath, checksum, sourceType, modId, modName, now, fileName);
    }

    public void insert(String fileName, String filePath, String checksum,
                       String sourceType, Long modId, String modName, String now) {
        ensureSchema();
        jdbc.update("""
                INSERT INTO key_catalog(file_name, file_path, checksum, source_type, source_mod_id,
                  source_mod_name, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?)
                """, fileName, filePath, checksum, sourceType, modId, modName, now, now);
    }

    public void deleteByFileName(String fileName) {
        ensureSchema();
        jdbc.update("DELETE FROM key_catalog WHERE file_name=?", fileName);
    }
}
