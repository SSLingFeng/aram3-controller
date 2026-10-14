package com.aram3.service;

import com.aram3.common.BusinessException;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.UserRepository;
import com.aram3.security.AuthFacade;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserAdminService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditRepository auditRepository;

    public UserAdminService(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            AuditRepository auditRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditRepository = auditRepository;
    }

    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.id());
            m.put("username", u.username());
            m.put("displayName", u.displayName());
            m.put("email", u.email());
            m.put("status", u.status());
            m.put("superAdmin", u.isSuperAdmin());
            m.put("lastLoginAt", u.lastLoginAt());
            m.put("createdAt", u.createdAt());
            m.put("roles", userRepository.findRoleCodes(u.id()));
            m.put("roleIds", userRepository.findRoleIds(u.id()));
            return m;
        }).toList();
    }

    @Transactional
    public Map<String, Object> createUser(String username, String password, String displayName,
                                          String email, boolean superAdmin, List<Long> roleIds) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("用户名已存在");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException("密码至少 6 位");
        }
        long id = userRepository.create(username, passwordEncoder.encode(password), displayName, superAdmin);
        if (email != null && !email.isBlank()) {
            userRepository.updateProfile(id, displayName, email, superAdmin);
        }
        if (roleIds != null && !roleIds.isEmpty()) {
            userRepository.replaceRoles(id, roleIds);
        }
        auditRepository.log(currentUserId(), "user.create", "user", id, username, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> updateProfile(long id, String displayName, String email,
                                             Boolean superAdmin, List<Long> roleIds) {
        requireUser(id);
        if (Boolean.FALSE.equals(superAdmin)) {
            // prevent removing the last super admin
            if (userRepository.findById(id).map(u -> u.isSuperAdmin()).orElse(false)
                    && userRepository.countAdmins() <= 1) {
                throw new BusinessException("至少保留一名超级管理员");
            }
        }
        userRepository.updateProfile(id, displayName, email, superAdmin);
        if (roleIds != null) {
            userRepository.replaceRoles(id, roleIds);
        }
        auditRepository.log(currentUserId(), "user.update", "user", id, null, null);
        return detail(id);
    }

    public void resetPassword(long id, String newPassword) {
        requireUser(id);
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码至少 6 位");
        }
        userRepository.updatePassword(id, passwordEncoder.encode(newPassword));
        auditRepository.log(currentUserId(), "user.reset_password", "user", id, null, null);
    }

    public void changeOwnPassword(String oldPassword, String newPassword) {
        String username = AuthFacade.currentUsername();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!passwordEncoder.matches(oldPassword, user.passwordHash())) {
            throw new BusinessException("原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码至少 6 位");
        }
        userRepository.updatePassword(user.id(), passwordEncoder.encode(newPassword));
        auditRepository.log(user.id(), "user.change_password", "user", user.id(), null, null);
    }

    public void updateStatus(long id, int status) {
        requireUser(id);
        if (status == 0) {
            if (userRepository.findById(id).map(u -> u.isSuperAdmin()).orElse(false)
                    && userRepository.countAdmins() <= 1) {
                throw new BusinessException("不能停用最后一名超级管理员");
            }
        }
        userRepository.updateStatus(id, status);
        auditRepository.log(currentUserId(), "user.status", "user", id, String.valueOf(status), null);
    }

    @Transactional
    public void delete(long id) {
        requireUser(id);
        Long self = currentUserId();
        if (self != null && self == id) {
            throw new BusinessException("不能删除当前登录账号");
        }
        if (userRepository.findById(id).map(u -> u.isSuperAdmin()).orElse(false)
                && userRepository.countAdmins() <= 1) {
            throw new BusinessException("不能删除最后一名超级管理员");
        }
        userRepository.delete(id);
        auditRepository.log(self, "user.delete", "user", id, null, null);
    }

    public Map<String, Object> detail(long id) {
        return userRepository.findById(id).map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.id());
            m.put("username", u.username());
            m.put("displayName", u.displayName());
            m.put("email", u.email());
            m.put("status", u.status());
            m.put("superAdmin", u.isSuperAdmin());
            m.put("lastLoginAt", u.lastLoginAt());
            m.put("createdAt", u.createdAt());
            m.put("roles", userRepository.findRoleCodes(u.id()));
            m.put("roleIds", userRepository.findRoleIds(u.id()));
            return m;
        }).orElseThrow(() -> new BusinessException("用户不存在"));
    }

    private void requireUser(long id) {
        userRepository.findById(id).orElseThrow(() -> new BusinessException("用户不存在"));
    }

    private Long currentUserId() {
        String username = AuthFacade.currentUsername();
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username).map(u -> u.id()).orElse(null);
    }
}
