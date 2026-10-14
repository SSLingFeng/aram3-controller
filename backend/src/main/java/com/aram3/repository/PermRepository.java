package com.aram3.repository;

import com.aram3.domain.UserInstancePerm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class PermRepository {
    private final JdbcTemplate jdbc;

    public PermRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<UserInstancePerm> MAPPER = (rs, i) -> new UserInstancePerm(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("instance_id"),
            rs.getInt("can_view"),
            rs.getInt("can_start_stop"),
            rs.getInt("can_edit_config"),
            rs.getInt("can_manage_map"),
            rs.getInt("can_manage_mod"),
            rs.getInt("can_transfer_files"),
            rs.getInt("can_view_logs"),
            rs.getInt("can_rcon"),
            rs.getInt("can_schedule")
    );

    public Optional<UserInstancePerm> find(long userId, long instanceId) {
        return jdbc.query("SELECT * FROM user_instance_perm WHERE user_id=? AND instance_id=?",
                MAPPER, userId, instanceId).stream().findFirst();
    }

    public List<UserInstancePerm> findByUser(long userId) {
        return jdbc.query("SELECT * FROM user_instance_perm WHERE user_id=?", MAPPER, userId);
    }

    public List<UserInstancePerm> findByInstance(long instanceId) {
        return jdbc.query("SELECT * FROM user_instance_perm WHERE instance_id=?", MAPPER, instanceId);
    }

    public void upsert(UserInstancePerm p) {
        Optional<UserInstancePerm> existing = find(p.userId(), p.instanceId());
        if (existing.isPresent()) {
            jdbc.update("""
                    UPDATE user_instance_perm SET can_view=?, can_start_stop=?, can_edit_config=?,
                      can_manage_map=?, can_manage_mod=?, can_transfer_files=?, can_view_logs=?,
                      can_rcon=?, can_schedule=?, updated_at=? WHERE id=?
                    """,
                    p.canView(), p.canStartStop(), p.canEditConfig(), p.canManageMap(), p.canManageMod(),
                    p.canTransferFiles(), p.canViewLogs(), p.canRcon(), p.canSchedule(),
                    Instant.now().toString(), existing.get().id());
        } else {
            jdbc.update("""
                    INSERT INTO user_instance_perm(user_id, instance_id, can_view, can_start_stop, can_edit_config,
                      can_manage_map, can_manage_mod, can_transfer_files, can_view_logs, can_rcon, can_schedule,
                      created_at, updated_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """,
                    p.userId(), p.instanceId(), p.canView(), p.canStartStop(), p.canEditConfig(),
                    p.canManageMap(), p.canManageMod(), p.canTransferFiles(), p.canViewLogs(), p.canRcon(),
                    p.canSchedule(), Instant.now().toString(), Instant.now().toString());
        }
    }

    public void delete(long userId, long instanceId) {
        jdbc.update("DELETE FROM user_instance_perm WHERE user_id=? AND instance_id=?", userId, instanceId);
    }
}


