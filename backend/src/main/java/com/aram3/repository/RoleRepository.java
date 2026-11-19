package com.aram3.repository;

import com.aram3.domain.Role;
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
public class RoleRepository {
    private final JdbcTemplate jdbc;

    public RoleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Role> MAPPER = (rs, i) -> new Role(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("is_system") == 1
    );

    public List<Role> findAll() {
        return jdbc.query("SELECT id, code, name, description, is_system FROM sys_role ORDER BY id", MAPPER);
    }

    public Optional<Role> findById(long id) {
        return jdbc.query("SELECT id, code, name, description, is_system FROM sys_role WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public boolean existsByCode(String code) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role WHERE code = ?", Integer.class, code);
        return n != null && n > 0;
    }

    public long create(String code, String name, String description) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO sys_role(code, name, description, is_system, created_at) VALUES (?,?,?,0,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, Instant.now().toString());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void update(long id, String name, String description) {
        jdbc.update("UPDATE sys_role SET name = ?, description = ? WHERE id = ?", name, description, id);
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM sys_role WHERE id = ?", id);
    }

    /** 角色的权限码列表（保持插入顺序）。 */
    public List<String> findPermCodes(long roleId) {
        return jdbc.query("SELECT perm_code FROM sys_role_permission WHERE role_id = ? ORDER BY rowid",
                (rs, i) -> rs.getString(1), roleId);
    }

    /** 用户经全部角色聚合去重后的权限码。 */
    public List<String> findPermCodesByUser(long userId) {
        return jdbc.query("""
                        SELECT DISTINCT rp.perm_code FROM sys_role_permission rp
                        JOIN sys_user_role ur ON ur.role_id = rp.role_id
                        WHERE ur.user_id = ?
                        """,
                (rs, i) -> rs.getString(1), userId);
    }

    /** 整体替换角色权限码。 */
    public void replacePerms(long roleId, List<String> codes) {
        jdbc.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
        if (codes == null) {
            return;
        }
        for (String code : codes) {
            jdbc.update("INSERT INTO sys_role_permission(role_id, perm_code) VALUES (?,?)", roleId, code);
        }
    }

    /** 引用该角色的用户数（删除前检查）。 */
    public int countUsersWithRole(long roleId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user_role WHERE role_id = ?", Integer.class, roleId);
        return n == null ? 0 : n;
    }

    /** 权限码总行数（判断是否首次播种）。 */
    public int countPermRows() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_permission", Integer.class);
        return n == null ? 0 : n;
    }
}
