package com.aram3.service;

import com.aram3.common.BusinessException;
import com.aram3.domain.SysUser;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.UserRepository;
import com.aram3.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
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

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       AuditRepository auditRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.auditRepository = auditRepository;
    }

    public Map<String, Object> login(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        userRepository.updateLastLogin(user.id());
        String access = jwtService.createAccessToken(user.id(), user.username(), user.isSuperAdmin());
        String refresh = jwtService.createRefreshToken(user.id(), user.username());
        auditRepository.log(user.id(), "auth.login", "user", user.id(), null, null);
        Map<String, Object> body = new HashMap<>();
        body.put("accessToken", access);
        body.put("refreshToken", refresh);
        body.put("tokenType", "Bearer");
        body.put("user", publicUser(user));
        return body;
    }

    public Map<String, Object> refresh(String refreshToken) {
        var claims = jwtService.parse(refreshToken);
        if (!jwtService.isRefreshToken(claims)) {
            throw new BusinessException(401, "无效的 refresh token");
        }
        SysUser user = userRepository.findByUsername(claims.getSubject())
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
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
        m.put("lastLoginAt", user.lastLoginAt());
        m.put("createdAt", user.createdAt());
        return m;
    }
}
