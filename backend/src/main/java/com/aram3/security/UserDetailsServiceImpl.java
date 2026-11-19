package com.aram3.security;

import com.aram3.domain.SysUser;
import com.aram3.repository.RoleRepository;
import com.aram3.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserDetailsServiceImpl(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found: " + username));
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        userRepository.findRoleCodes(u.id()).forEach(code ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + code)));
        if (u.isSuperAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_admin"));
            // 超管直接持有全部权限码，hasAuthority 校验天然放行
            for (PermCode pc : PermCode.values()) {
                authorities.add(new SimpleGrantedAuthority(pc.code()));
            }
        } else {
            // 角色权限码（无前缀），供 hasAuthority('xxx') 校验
            roleRepository.findPermCodesByUser(u.id()).forEach(code ->
                    authorities.add(new SimpleGrantedAuthority(code)));
        }
        return new AppPrincipal(u.username(), u.passwordHash(), u.status() == 1, !u.locked(),
                authorities, u.id(), u.isSuperAdmin());
    }
}
