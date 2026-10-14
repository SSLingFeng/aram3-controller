package com.aram3.repository;

// TimeMapper is same package

import com.aram3.domain.MapCatalog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class MapRepository {
    private final JdbcTemplate jdbc;

    public MapRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<MapCatalog> MAPPER = (rs, i) -> new MapCatalog(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("map_key"),
            rs.getString("file_name"),
            rs.getString("file_path"),
            DbTypes.getLong(rs, "file_size"),
            rs.getString("checksum"),
            rs.getString("source_type"),
            DbTypes.getLong(rs, "created_by"),
            TimeMapper.getInstant(rs, "created_at"),
            TimeMapper.getInstant(rs, "updated_at")
    );


    public List<MapCatalog> findAll() {
        return jdbc.query("SELECT * FROM map_catalog ORDER BY id", MAPPER);
    }

    public Optional<MapCatalog> findById(long id) {
        return jdbc.query("SELECT * FROM map_catalog WHERE id=?", MAPPER, id).stream().findFirst();
    }

    public long create(MapCatalog map) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO map_catalog(name, map_key, file_name, file_path, file_size, checksum,
                      source_type, created_by, created_at, updated_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, map.name());
            ps.setString(2, map.mapKey());
            ps.setString(3, map.fileName());
            ps.setString(4, map.filePath());
            ps.setObject(5, map.fileSize());
            ps.setString(6, map.checksum());
            ps.setString(7, map.sourceType() == null ? "upload" : map.sourceType());
            ps.setObject(8, map.createdBy());
            ps.setString(9, Instant.now().toString());
            ps.setString(10, Instant.now().toString());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM map_catalog WHERE id=?", id);
    }

    public void bindInstanceMap(long instanceId, long mapId, boolean active, int sortOrder) {
        Instant now = Instant.now();
        if (active) {
            jdbc.update("UPDATE instance_map SET is_active=0 WHERE instance_id=?", instanceId);
        }
        int updated = jdbc.update("""
                UPDATE instance_map SET is_active=?, sort_order=? WHERE instance_id=? AND map_id=?
                """, active ? 1 : 0, sortOrder, instanceId, mapId);
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO instance_map(instance_id, map_id, is_active, sort_order, created_at)
                    VALUES (?,?,?,?,?)
                    """, instanceId, mapId, active ? 1 : 0, sortOrder, now.toString());
        }
    }

    public List<MapCatalog> findInstanceMaps(long instanceId) {
        return jdbc.query("""
                SELECT m.* FROM map_catalog m JOIN instance_map im ON im.map_id = m.id
                WHERE im.instance_id = ? ORDER BY im.sort_order, im.id
                """, MAPPER, instanceId);
    }

    public void unbind(long instanceId, long mapId) {
        jdbc.update("DELETE FROM instance_map WHERE instance_id=? AND map_id=?", instanceId, mapId);
    }
}


