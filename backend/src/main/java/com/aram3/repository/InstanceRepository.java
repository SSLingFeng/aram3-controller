package com.aram3.repository;

// TimeMapper is same package

import com.aram3.domain.GameInstance;
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
public class InstanceRepository {
    private final JdbcTemplate jdbc;

    public InstanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<GameInstance> MAPPER = (rs, i) -> new GameInstance(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("root_path"),
            DbTypes.getIntObj(rs, "game_port"),
            DbTypes.getIntObj(rs, "steam_query_port"),
            rs.getString("bind_ip"),
            rs.getString("rcon_host"),
            DbTypes.getIntObj(rs, "rcon_port"),
            rs.getString("rcon_password"),
            DbTypes.getIntObj(rs, "max_players"),
            rs.getString("profile_name"),
            rs.getString("start_script"),
            rs.getString("extra_args"),
            rs.getString("status"),
            rs.getInt("enabled"),
            DbTypes.getLong(rs, "created_by"),
            TimeMapper.getInstant(rs, "created_at"),
            TimeMapper.getInstant(rs, "updated_at")
    );


    public List<GameInstance> findAll() {
        return jdbc.query("SELECT * FROM game_instance ORDER BY id", MAPPER);
    }

    public Optional<GameInstance> findById(long id) {
        return jdbc.query("SELECT * FROM game_instance WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<GameInstance> findByCode(String code) {
        return jdbc.query("SELECT * FROM game_instance WHERE code = ?", MAPPER, code).stream().findFirst();
    }

    public long create(GameInstance inst) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO game_instance(code, name, description, root_path, game_port, steam_query_port,
                      bind_ip, rcon_host, rcon_port, rcon_password, max_players, profile_name, start_script, extra_args,
                      status, enabled, created_by, created_at, updated_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?, 'STOPPED',1,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, inst.code());
            ps.setString(2, inst.name());
            ps.setString(3, inst.description());
            ps.setString(4, inst.rootPath());
            ps.setObject(5, inst.gamePort());
            ps.setObject(6, inst.steamQueryPort());
            ps.setString(7, inst.bindIp());
            ps.setString(8, inst.rconHost());
            ps.setObject(9, inst.rconPort());
            ps.setString(10, inst.rconPassword());
            ps.setObject(11, inst.maxPlayers());
            ps.setString(12, inst.profileName());
            ps.setString(13, inst.startScript());
            ps.setString(14, inst.extraArgs());
            ps.setObject(15, inst.createdBy());
            ps.setString(16, Instant.now().toString());
            ps.setString(17, Instant.now().toString());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void update(GameInstance inst) {
        jdbc.update("""
                UPDATE game_instance SET name=?, description=?, root_path=?, game_port=?, steam_query_port=?,
                  bind_ip=?, rcon_host=?, rcon_port=?, rcon_password=?, max_players=?, profile_name=?, start_script=?,
                  extra_args=?, enabled=?, updated_at=? WHERE id=?
                """,
                inst.name(), inst.description(), inst.rootPath(), inst.gamePort(), inst.steamQueryPort(),
                inst.bindIp(), inst.rconHost(), inst.rconPort(), inst.rconPassword(), inst.maxPlayers(), inst.profileName(),
                inst.startScript(), inst.extraArgs(), inst.enabled(), Instant.now().toString(), inst.id());
    }

    public void updateStatus(long id, String status) {
        jdbc.update("UPDATE game_instance SET status = ?, updated_at = ? WHERE id = ?",
                status, Instant.now().toString(), id);
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM game_instance WHERE id = ?", id);
    }
}