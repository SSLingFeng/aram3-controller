package com.aram3.repository;

import com.aram3.domain.SysUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<SysUser> MAPPER = (rs, i) -> new SysUser(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("display_name"),
            rs.getString("email"),
            rs.getInt("status"),
            rs.getInt("is_super_admin") == 1,
            rs.getInt("login_fail_count"),
            rs.getInt("locked") == 1,
            TimeMapper.getInstant(rs, "last_login_at"),
            TimeMapper.getInstant(rs, "created_at"),
            TimeMapper.getInstant(rs, "updated_at")
    );

    public Optional<SysUser> findByUsername(String username) {
        return jdbc.query("SELECT * FROM sys_user WHERE username = ?", MAPPER, username).stream().findFirst();
    }

    public Optional<SysUser> findById(long id) {
        return jdbc.query("SELECT * FROM sys_user WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public List<SysUser> findAll() {
        return jdbc.query("SELECT * FROM sys_user ORDER BY id", MAPPER);
    }

    public long create(String username, String passwordHash, String displayName, boolean superAdmin) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO sys_user(username, password_hash, display_name, status, is_super_admin, created_at, updated_at) VALUES (?,?,?,1,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, displayName);
            ps.setInt(4, superAdmin ? 1 : 0);
            ps.setString(5, Instant.now().toString());
            ps.setString(6, Instant.now().toString());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void updateProfile(long id, String displayName, String email, Boolean superAdmin) {
        jdbc.update("""
                UPDATE sys_user SET
                  display_name = COALESCE(?, display_name),
                  email = COALESCE(?, email),
                  is_super_admin = COALESCE(?, is_super_admin),
                  updated_at = ?
                WHERE id = ?
                """,
                displayName, email,
                superAdmin == null ? null : (superAdmin ? 1 : 0),
                Instant.now().toString(), id);
    }

    public void updatePassword(long id, String passwordHash) {
        jdbc.update("UPDATE sys_user SET password_hash = ?, updated_at = ? WHERE id = ?",
                passwordHash, Instant.now().toString(), id);
    }

    public void updateLastLogin(long id) {
        jdbc.update("UPDATE sys_user SET last_login_at = ? WHERE id = ?", Instant.now().toString(), id);
    }

    public void updateStatus(long id, int status) {
        jdbc.update("UPDATE sys_user SET status = ?, updated_at = ? WHERE id = ?",
                status, Instant.now().toString(), id);
    }

    /** 登录失败计数 +1，返回累计次数。 */
    public int increaseLoginFailCount(long id) {
        jdbc.update("UPDATE sys_user SET login_fail_count = login_fail_count + 1, updated_at = ? WHERE id = ?",
                Instant.now().toString(), id);
        Integer n = jdbc.queryForObject("SELECT login_fail_count FROM sys_user WHERE id = ?", Integer.class, id);
        return n == null ? 0 : n;
    }

    public void resetLoginFailCount(long id) {
        jdbc.update("UPDATE sys_user SET login_fail_count = 0, updated_at = ? WHERE id = ?",
                Instant.now().toString(), id);
    }

    public void setLocked(long id, boolean locked) {
        jdbc.update("UPDATE sys_user SET locked = ?, updated_at = ? WHERE id = ?",
                locked ? 1 : 0, Instant.now().toString(), id);
    }

    /** 解锁并清空登录失败计数。 */
    public void unlock(long id) {
        jdbc.update("UPDATE sys_user SET locked = 0, login_fail_count = 0, updated_at = ? WHERE id = ?",
                Instant.now().toString(), id);
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM sys_user WHERE id = ?", id);
    }

    public List<String> findRoleCodes(long userId) {
        return jdbc.query(
                "SELECT r.code FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = ?",
                (rs, i) -> rs.getString(1), userId);
    }

    public List<Long> findRoleIds(long userId) {
        return jdbc.query("SELECT role_id FROM sys_user_role WHERE user_id = ?",
                (rs, i) -> rs.getLong(1), userId);
    }

    public void replaceRoles(long userId, List<Long> roleIds) {
        jdbc.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleId != null) {
                jdbc.update("INSERT INTO sys_user_role(user_id, role_id) VALUES (?,?)", userId, roleId);
            }
        }
    }

    public List<java.util.Map<String, Object>> findRoles() {
        return jdbc.queryForList("SELECT id, code, name, description FROM sys_role ORDER BY id");
    }

    public int countAdmins() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE is_super_admin = 1 AND status = 1", Integer.class);
        return n == null ? 0 : n;
    }
}
