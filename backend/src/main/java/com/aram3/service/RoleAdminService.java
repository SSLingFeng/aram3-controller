package com.aram3.service;

import com.aram3.common.BusinessException;
import com.aram3.domain.Role;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.RoleRepository;
import com.aram3.security.AuthFacade;
import com.aram3.security.PermCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoleAdminService {
    private final RoleRepository roleRepository;
    private final AuditRepository auditRepository;

    public RoleAdminService(RoleRepository roleRepository, AuditRepository auditRepository) {
        this.roleRepository = roleRepository;
        this.auditRepository = auditRepository;
    }

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Role r : roleRepository.findAll()) {
            result.add(toView(r));
        }
        return result;
    }

    public Map<String, Object> detail(long id) {
        return toView(requireRole(id));
    }

    /** 权限码目录（分组返回，前端据此渲染「系统权限」「特殊权限（实例）」两组复选框）。 */
    public List<Map<String, Object>> permCatalog() {
        List<Map<String, Object>> catalog = new ArrayList<>();
        for (PermCode pc : PermCode.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", pc.code());
            m.put("group", pc.group().name());
            catalog.add(m);
        }
        return catalog;
    }

    @Transactional
    public Map<String, Object> create(String code, String name, String description, List<String> perms) {
        if (code == null || !code.matches("[a-z0-9_]{2,32}")) {
            throw new BusinessException("角色 code 仅允许小写字母数字下划线，2-32 位");
        }
        if (name == null || name.isBlank()) {
            throw new BusinessException("角色名称不能为空");
        }
        if (roleRepository.existsByCode(code)) {
            throw new BusinessException("角色 code 已存在");
        }
        validatePerms(perms);
        long id = roleRepository.create(code, name.trim(), description);
        roleRepository.replacePerms(id, perms);
        auditRepository.log(AuthFacade.currentUserId(), "role.create", "role", id, code, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> update(long id, String name, String description, List<String> perms) {
        Role role = requireRole(id);
        if (name == null || name.isBlank()) {
            throw new BusinessException("角色名称不能为空");
        }
        validatePerms(perms);
        roleRepository.update(id, name.trim(), description);
        roleRepository.replacePerms(id, perms);
        auditRepository.log(AuthFacade.currentUserId(), "role.update", "role", id, role.code(), null);
        return detail(id);
    }

    @Transactional
    public void delete(long id) {
        Role role = requireRole(id);
        if (role.system()) {
            throw new BusinessException("系统内置角色不允许删除");
        }
        int users = roleRepository.countUsersWithRole(id);
        if (users > 0) {
            throw new BusinessException("仍有 " + users + " 个用户使用该角色，请先移除后再删除");
        }
        roleRepository.delete(id);
        auditRepository.log(AuthFacade.currentUserId(), "role.delete", "role", id, role.code(), null);
    }

    private Map<String, Object> toView(Role r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.id());
        m.put("code", r.code());
        m.put("name", r.name());
        m.put("description", r.description());
        m.put("isSystem", r.system());
        m.put("perms", roleRepository.findPermCodes(r.id()));
        return m;
    }

    private Role requireRole(long id) {
        return roleRepository.findById(id).orElseThrow(() -> new BusinessException("角色不存在"));
    }

    private void validatePerms(List<String> perms) {
        if (perms == null) {
            return;
        }
        for (String code : perms) {
            if (!PermCode.isValid(code)) {
                throw new BusinessException("未知权限码: " + code);
            }
        }
    }
}
