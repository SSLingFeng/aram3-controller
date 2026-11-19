package com.aram3.security;

import java.util.Arrays;
import java.util.List;

/**
 * 全局权限码目录（角色级权限，存于 sys_role_permission）。
 * 实例级权限（user_instance_perm 的 10 个开关）不在此列。
 */
public enum PermCode {
    USER_MANAGE("user:manage", Group.SYSTEM),
    SYSTEM_SETTINGS("system:settings", Group.SYSTEM),
    JOB_MANAGE("job:manage", Group.SYSTEM),
    MAP_MANAGE("map:manage", Group.SYSTEM),
    MOD_MANAGE("mod:manage", Group.SYSTEM),
    STEAMCMD_USE("steamcmd:use", Group.SYSTEM),
    AUDIT_VIEW("audit:view", Group.SYSTEM),
    INSTANCE_CREATE("instance:create", Group.INSTANCE),
    INSTANCE_GRANT("instance:grant", Group.INSTANCE);

    public enum Group { SYSTEM, INSTANCE }

    private final String code;
    private final Group group;

    PermCode(String code, Group group) {
        this.code = code;
        this.group = group;
    }

    public String code() {
        return code;
    }

    public Group group() {
        return group;
    }

    /** 全部权限码（超管 authorities / 前端目录用）。 */
    public static List<String> allCodes() {
        return Arrays.stream(values()).map(PermCode::code).toList();
    }

    /** 是否为目录内的合法权限码。 */
    public static boolean isValid(String code) {
        return allCodes().contains(code);
    }
}
