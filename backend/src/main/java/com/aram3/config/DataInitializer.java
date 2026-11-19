package com.aram3.config;

import com.aram3.repository.RoleRepository;
import com.aram3.repository.UserRepository;
import com.aram3.security.PermCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** 首次创建 admin 使用的默认密码（开发便利；生产环境登录后请立即修改） */
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";

    private static final String ROLE_PERMS_MIGRATION_FLAG = "migrated.rolePerms.v2";

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(JdbcTemplate jdbc, UserRepository userRepository,
                           RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureBindIpColumn();
        ensureLoginColumns();
        ensureCanEditCoreColumn();
        seedRoles();
        migrateRolePermsOnce();
        seedAdmin();
        log.info("Aram3 data initialization done");
    }

    private void ensureBindIpColumn() {
        try {
            jdbc.execute("ALTER TABLE game_instance ADD COLUMN bind_ip TEXT");
        } catch (Exception ignored) {
            // column already exists
        }
    }

    private void ensureLoginColumns() {
        try {
            jdbc.execute("ALTER TABLE sys_user ADD COLUMN login_fail_count INTEGER NOT NULL DEFAULT 0");
        } catch (Exception ignored) {
            // column already exists
        }
        try {
            jdbc.execute("ALTER TABLE sys_user ADD COLUMN locked INTEGER NOT NULL DEFAULT 0");
        } catch (Exception ignored) {
            // column already exists
        }
    }

    private void ensureCanEditCoreColumn() {
        try {
            jdbc.execute("ALTER TABLE user_instance_perm ADD COLUMN can_edit_core INTEGER NOT NULL DEFAULT 0");
        } catch (Exception ignored) {
            // column already exists
        }
    }

    private void seedRoles() {
        jdbc.update("INSERT OR IGNORE INTO sys_role(code, name, description, is_system, created_at) VALUES (?,?,?,?,?)",
                "admin", "系统管理员", "全部系统权限", 1, Instant.now().toString());
        jdbc.update("INSERT OR IGNORE INTO sys_role(code, name, description, is_system, created_at) VALUES (?,?,?,?,?)",
                "operator", "运维操作员", "实例启停与配置", 1, Instant.now().toString());
        jdbc.update("INSERT OR IGNORE INTO sys_role(code, name, description, is_system, created_at) VALUES (?,?,?,?,?)",
                "mod_admin", "模组管理员", "地图与模组管理", 1, Instant.now().toString());
        jdbc.update("INSERT OR IGNORE INTO sys_role(code, name, description, is_system, created_at) VALUES (?,?,?,?,?)",
                "viewer", "只读用户", "仅查看", 1, Instant.now().toString());

        // 权限码仅在表为空（首次安装）时播种默认值，之后完全交给「角色与权限」界面维护，
        // 避免每次启动把管理员手动取消的权限又加回来
        if (roleRepository.countPermRows() == 0) {
            for (PermCode pc : PermCode.values()) {
                ensurePerm("admin", pc.code());
            }
            ensurePerm("operator", PermCode.JOB_MANAGE.code());
            ensurePerm("operator", PermCode.STEAMCMD_USE.code());
            ensurePerm("mod_admin", PermCode.MAP_MANAGE.code());
            ensurePerm("mod_admin", PermCode.MOD_MANAGE.code());
        }
    }

    /** 旧库一次性迁移：给 admin 角色补上后续版本新增的权限码。 */
    private void migrateRolePermsOnce() {
        Integer done = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_setting WHERE key = ?", Integer.class, ROLE_PERMS_MIGRATION_FLAG);
        if (done != null && done > 0) {
            return;
        }
        for (PermCode pc : List.of(PermCode.MAP_MANAGE, PermCode.MOD_MANAGE, PermCode.STEAMCMD_USE)) {
            ensurePerm("admin", pc.code());
        }
        jdbc.update("INSERT INTO sys_setting(key, value, description, updated_at) VALUES (?,?,?,?)",
                ROLE_PERMS_MIGRATION_FLAG, "1", "角色权限码 v2 一次性迁移标记", Instant.now().toString());
        log.info("已为 admin 角色补充新权限码（一次性迁移）");
    }

    private void ensurePerm(String roleCode, String perm) {
        jdbc.update("""
                INSERT OR IGNORE INTO sys_role_permission(role_id, perm_code)
                SELECT id, ? FROM sys_role WHERE code = ?
                """, perm, roleCode);
    }

    private void seedAdmin() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            long uid = userRepository.create("admin", passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD), "系统管理员", true);
            jdbc.update("INSERT OR IGNORE INTO sys_user_role(user_id, role_id) SELECT ?, id FROM sys_role WHERE code = ?",
                    uid, "admin");
            log.warn("==========================================================");
            log.warn("  首次启动已创建管理员账号（默认密码，请登录后立即修改）");
            log.warn("  用户名: admin");
            log.warn("  密码:   {}", DEFAULT_ADMIN_PASSWORD);
            log.warn("==========================================================");
        } else {
            jdbc.update("INSERT OR IGNORE INTO sys_user_role(user_id, role_id) SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = ? AND r.code = ?",
                    "admin", "admin");
        }
    }
}
