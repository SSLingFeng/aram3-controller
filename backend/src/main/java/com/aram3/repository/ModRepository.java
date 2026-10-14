package com.aram3.repository;

// TimeMapper is same package

import com.aram3.domain.InstanceMod;
import com.aram3.domain.ModCatalog;
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
public class ModRepository {
    private final JdbcTemplate jdbc;

    public ModRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<ModCatalog> MOD_MAPPER = (rs, i) -> new ModCatalog(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("workshop_id"),
            rs.getString("author"),
            rs.getString("version"),
            rs.getString("file_name"),
            rs.getString("file_path"),
            DbTypes.getLong(rs, "file_size"),
            rs.getString("checksum"),
            rs.getString("default_load_type"),
            rs.getString("download_status"),
            rs.getString("description"),
            DbTypes.getLong(rs, "created_by"),
            TimeMapper.getInstant(rs, "created_at"),
            TimeMapper.getInstant(rs, "updated_at")
    );

    private static final RowMapper<InstanceMod> INST_MOD_MAPPER = (rs, i) -> new InstanceMod(
            rs.getLong("id"),
            rs.getLong("instance_id"),
            rs.getLong("mod_id"),
            rs.getString("load_type"),
            rs.getInt("enabled"),
            rs.getInt("load_order"),
            rs.getString("custom_args"),
            TimeMapper.getInstant(rs, "installed_at"),
            TimeMapper.getInstant(rs, "updated_at")
    );


    public List<ModCatalog> findAllMods() {
        return jdbc.query("SELECT * FROM mod_catalog ORDER BY id", MOD_MAPPER);
    }

    public Optional<ModCatalog> findModById(long id) {
        return jdbc.query("SELECT * FROM mod_catalog WHERE id=?", MOD_MAPPER, id).stream().findFirst();
    }

    public Optional<ModCatalog> findModByWorkshopId(String workshopId) {
        return jdbc.query("SELECT * FROM mod_catalog WHERE workshop_id=?", MOD_MAPPER, workshopId).stream().findFirst();
    }

    public long createMod(ModCatalog mod) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO mod_catalog(name, workshop_id, author, version, file_name, file_path, file_size,
                      checksum, default_load_type, download_status, description, created_by, created_at, updated_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, mod.name());
            ps.setString(2, mod.workshopId());
            ps.setString(3, mod.author());
            ps.setString(4, mod.version());
            ps.setString(5, mod.fileName());
            ps.setString(6, mod.filePath());
            ps.setObject(7, mod.fileSize());
            ps.setString(8, mod.checksum());
            ps.setString(9, mod.defaultLoadType() == null ? "BOTH" : mod.defaultLoadType());
            ps.setString(10, mod.downloadStatus() == null ? "PENDING" : mod.downloadStatus());
            ps.setString(11, mod.description());
            ps.setObject(12, mod.createdBy());
            ps.setString(13, Instant.now().toString());
            ps.setString(14, Instant.now().toString());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void updateModDownload(long id, String status, String filePath, String fileName, Long fileSize) {
        jdbc.update("UPDATE mod_catalog SET download_status=?, file_path=?, file_name=?, file_size=?, updated_at=? WHERE id=?",
                status, filePath, fileName, fileSize, Instant.now().toString(), id);
    }

    public void deleteMod(long id) {
        jdbc.update("DELETE FROM mod_catalog WHERE id=?", id);
    }

    public List<InstanceMod> findInstanceMods(long instanceId) {
        return jdbc.query("SELECT * FROM instance_mod WHERE instance_id=? ORDER BY load_order, id",
                INST_MOD_MAPPER, instanceId);
    }

    public void bindInstanceMod(long instanceId, long modId, String loadType, int enabled, int loadOrder) {
        Instant now = Instant.now();
        int updated = jdbc.update("""
                UPDATE instance_mod SET load_type=?, enabled=?, load_order=?, updated_at=?
                WHERE instance_id=? AND mod_id=?
                """, loadType, enabled, loadOrder, now.toString(), instanceId, modId);
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO instance_mod(instance_id, mod_id, load_type, enabled, load_order, installed_at, updated_at)
                    VALUES (?,?,?,?,?,?,?)
                    """, instanceId, modId, loadType, enabled, loadOrder, now.toString(), now.toString());
        }
    }

    public void unbindInstanceMod(long instanceId, long modId) {
        jdbc.update("DELETE FROM instance_mod WHERE instance_id=? AND mod_id=?", instanceId, modId);
    }

    public int countModReferences(long modId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM instance_mod WHERE mod_id=?", Integer.class, modId);
        return n == null ? 0 : n;
    }
}


