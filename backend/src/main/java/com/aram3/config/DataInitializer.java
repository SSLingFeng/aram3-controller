package com.aram3.config;

import com.aram3.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;

@Component
public class DataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(JdbcTemplate jdbc, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureBindIpColumn();
        ensureLoginColumns();
        seedRoles();
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

    private void seedRoles() {
        jdbc.update("INSERT OR IGNORE INTO sys_role(code, name, description, is_system, created_at) VALUES (?,?,?,?,?)",
                "admin", "系统管理员", "全部系统权限", 1, Instant.now().toString());
        jdbc.update("INSERT OR IGNORE INTO sys_role(code, name, description, is_system, created_at) VALUES (?,?,?,?,?)",
                "operator", "运维操作员", "实例启停与配置", 1, Instant.now().toString());
        jdbc.update("INSERT OR IGNORE INTO sys_role(code, name, description, is_system, created_at) VALUES (?,?,?,?,?)",
                "mod_admin", "模组管理员", "地图与模组管理", 1, Instant.now().toString());
        jdbc.update("INSERT OR IGNORE INTO sys_role(code, name, description, is_system, created_at) VALUES (?,?,?,?,?)",
                "viewer", "只读用户", "仅查看", 1, Instant.now().toString());

        ensurePerm("admin", "user:manage");
        ensurePerm("admin", "system:settings");
        ensurePerm("admin", "instance:create");
        ensurePerm("admin", "instance:grant");
        ensurePerm("admin", "job:manage");
        ensurePerm("admin", "audit:view");
    }

    private void ensurePerm(String roleCode, String perm) {
        jdbc.update("""
                INSERT OR IGNORE INTO sys_role_permission(role_id, perm_code)
                SELECT id, ? FROM sys_role WHERE code = ?
                """, perm, roleCode);
    }

    private void seedAdmin() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            String password = randomPassword();
            long uid = userRepository.create("admin", passwordEncoder.encode(password), "系统管理员", true);
            jdbc.update("INSERT OR IGNORE INTO sys_user_role(user_id, role_id) SELECT ?, id FROM sys_role WHERE code = ?",
                    uid, "admin");
            log.warn("==========================================================");
            log.warn("  首次启动已创建管理员账号（请立即登录并修改密码）");
            log.warn("  用户名: admin");
            log.warn("  密码:   {}", password);
            log.warn("==========================================================");
        } else {
            jdbc.update("INSERT OR IGNORE INTO sys_user_role(user_id, role_id) SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = ? AND r.code = ?",
                    "admin", "admin");
        }
    }

    /** 6 位密码：大写 + 小写 + 数字 + 符号（另 2 位随机，整体打乱） */
    private String randomPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnpqrstuvwxyz";
        String digit = "23456789";
        String symbol = "!@#$%^*?-_=+";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt(rnd.nextInt(upper.length())));
        sb.append(lower.charAt(rnd.nextInt(lower.length())));
        sb.append(digit.charAt(rnd.nextInt(digit.length())));
        sb.append(symbol.charAt(rnd.nextInt(symbol.length())));
        String all = upper + lower + digit + symbol;
        for (int i = 0; i < 2; i++) {
            sb.append(all.charAt(rnd.nextInt(all.length())));
        }
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
