package com.aram3.service;

import com.aram3.common.BusinessException;
import com.aram3.domain.SysUser;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.UserRepository;
import com.aram3.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditRepository auditRepository;
    private final SettingService settingService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       AuditRepository auditRepository,
                       SettingService settingService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.auditRepository = auditRepository;
        this.settingService = settingService;
    }

    /** 仅允许服务器本机登录的账户（无登录失败次数限制）。 */
    private static final String LOCAL_ONLY_USER = "admin";

    public Map<String, Object> login(String username, String password, String clientIp) {
        SysUser user = userRepository.findByUsername(username).orElse(null);

        if (LOCAL_ONLY_USER.equals(username) && !isLocalAddress(clientIp)) {
            auditRepository.log(user == null ? null : user.id(), "auth.login_denied", "user",
                    user == null ? null : user.id(), "admin 仅允许本机登录", clientIp);
            throw new BusinessException("admin 账户仅允许在服务器本机登录");
        }
        if (user != null && user.locked()) {
            throw new BusinessException("账户已锁定，请联系超级管理员在「用户管理」中解锁并重置密码");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (BadCredentialsException e) {
            if (user != null && !LOCAL_ONLY_USER.equals(username)) {
                int fails = userRepository.increaseLoginFailCount(user.id());
                int maxFails = settingService.maxLoginFailures();
                auditRepository.log(null, "auth.login_failed", "user", user.id(),
                        "登录失败第 " + fails + " 次（上限 " + maxFails + " 次）", clientIp);
                if (fails >= maxFails) {
                    userRepository.setLocked(user.id(), true);
                    auditRepository.log(null, "auth.locked", "user", user.id(),
                            "连续登录失败 " + fails + " 次，账户已锁定", clientIp);
                    throw new BusinessException("用户名或密码错误，连续失败 " + fails + " 次，账户已锁定");
                }
                throw new BusinessException("用户名或密码错误，连续失败 " + fails + "/" + maxFails + " 次后将锁定账户");
            }
            throw new BusinessException("用户名或密码错误");
        } catch (LockedException e) {
            throw new BusinessException("账户已锁定，请联系超级管理员在「用户管理」中解锁并重置密码");
        } catch (DisabledException e) {
            throw new BusinessException("账户已被禁用");
        }

        if (user == null) {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        }
        if (!LOCAL_ONLY_USER.equals(username)) {
            userRepository.resetLoginFailCount(user.id());
        }
        userRepository.updateLastLogin(user.id());
        String access = jwtService.createAccessToken(user.id(), user.username(), user.isSuperAdmin());
        String refresh = jwtService.createRefreshToken(user.id(), user.username());
        auditRepository.log(user.id(), "auth.login", "user", user.id(), null, clientIp);
        Map<String, Object> body = new HashMap<>();
        body.put("accessToken", access);
        body.put("refreshToken", refresh);
        body.put("tokenType", "Bearer");
        body.put("user", publicUser(user));
        return body;
    }

    /** 服务器本机地址判定（含 IPv4 回环段与 IPv6 回环）。 */
    private boolean isLocalAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String v = ip.trim();
        return v.equals("127.0.0.1") || v.equals("::1") || v.equals("0:0:0:0:0:0:0:1")
                || v.equalsIgnoreCase("localhost") || v.startsWith("127.");
    }

    public Map<String, Object> refresh(String refreshToken) {
        var claims = jwtService.parse(refreshToken);
        if (!jwtService.isRefreshToken(claims)) {
            throw new BusinessException(401, "无效的 refresh token");
        }
        SysUser user = userRepository.findByUsername(claims.getSubject())
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.locked() || user.status() != 1) {
            throw new BusinessException(401, "账户已被锁定或禁用");
        }
        String access = jwtService.createAccessToken(user.id(), user.username(), user.isSuperAdmin());
        Map<String, Object> body = new HashMap<>();
        body.put("accessToken", access);
        body.put("refreshToken", jwtService.createRefreshToken(user.id(), user.username()));
        body.put("tokenType", "Bearer");
        return body;
    }

    public Map<String, Object> me() {
        String username = com.aram3.security.AuthFacade.currentUsername();
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(401, "未登录"));
        Map<String, Object> body = new HashMap<>(publicUser(user));
        body.put("roles", userRepository.findRoleCodes(user.id()));
        body.put("permissions", Map.of("superAdmin", user.isSuperAdmin()));
        return body;
    }

    public Map<String, Object> publicUser(SysUser user) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.id());
        m.put("username", user.username());
        m.put("displayName", user.displayName());
        m.put("email", user.email());
        m.put("status", user.status());
        m.put("superAdmin", user.isSuperAdmin());
        m.put("loginFailCount", user.loginFailCount());
        m.put("locked", user.locked());
        m.put("lastLoginAt", user.lastLoginAt());
        m.put("createdAt", user.createdAt());
        return m;
    }
}
