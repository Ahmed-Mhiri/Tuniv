package com.tuniv.backend.config.security.services;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.tuniv.backend.user.model.User;

public class UserDetailsImpl implements UserDetails {

    private final User user;

    public UserDetailsImpl(User user) {
        this.user = user;
    }

    public Integer getId() {
        return user.getUserId();
    }
    
    public String getEmail() {
        return user.getEmail();
    }

    // --- THIS IS THE FIX ---
    // Added the missing getter for the profile photo URL.
    public String getProfilePhotoUrl() {
        return user.getProfilePhotoUrl();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // We can add roles here later if needed
        return Collections.emptyList();
    }

    // --- Standard UserDetails methods ---
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}