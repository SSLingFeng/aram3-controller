package com.aram3.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthFacade {
    private AuthFacade() {}

    public static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    /** 当前登录用户 ID（来自 AppPrincipal，无需再查库）。 */
    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppPrincipal principal) {
            return principal.getUid();
        }
        return null;
    }

    /** 当前用户是否持有某权限码（超管在 UserDetails 中已附加全部权限码）。 */
    public static boolean hasAuthority(String code) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> code.equals(a.getAuthority()));
    }

    /**
     * 超管仅由用户的 is_super_admin 标志（ROLE_SUPER_ADMIN）决定；
     * 拥有 admin 角色的普通用户不再等同超管。
     */
    public static boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }
}
