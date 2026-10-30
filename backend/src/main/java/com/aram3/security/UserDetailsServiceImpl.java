package com.aram3.security;

import com.aram3.domain.SysUser;
import com.aram3.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        }
        return User.builder()
                .username(u.username())
                .password(u.passwordHash())
                .authorities(authorities)
                .disabled(u.status() != 1)
                .accountLocked(u.locked())
                .build();
    }
}
