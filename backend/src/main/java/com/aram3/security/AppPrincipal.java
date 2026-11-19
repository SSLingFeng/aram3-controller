package com.aram3.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/** 携带用户 ID 与超管标志的 Principal，免去每次校验再查库。 */
public class AppPrincipal extends User {
    private final long uid;
    private final boolean superAdmin;

    public AppPrincipal(String username, String password, boolean enabled, boolean accountNonLocked,
                        Collection<? extends GrantedAuthority> authorities, long uid, boolean superAdmin) {
        super(username, password, enabled, true, true, accountNonLocked, authorities);
        this.uid = uid;
        this.superAdmin = superAdmin;
    }

    public long getUid() {
        return uid;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }
}
