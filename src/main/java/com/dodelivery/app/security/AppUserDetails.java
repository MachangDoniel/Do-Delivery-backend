package com.dodelivery.app.security;

import com.dodelivery.app.enums.Role;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Custom UserDetails that carries the user's UUID and Role so controllers
 * can inject it via @AuthenticationPrincipal without extra DB lookups.
 */
@Getter
@Builder
public class AppUserDetails implements UserDetails {

    private final UUID userId;
    private final String phone;
    private final Role role;
    private final List<GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /** OTP-based auth — no stored password. */
    @Override
    public String getPassword() {
        return null;
    }

    /** Phone number is the unique principal identifier. */
    @Override
    public String getUsername() {
        return phone;
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
