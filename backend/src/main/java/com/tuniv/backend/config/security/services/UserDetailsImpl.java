package com.tuniv.backend.config.security.services;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.tuniv.backend.user.model.User; // <-- Import List

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

    // --- FIX #1: Grant a default role to every user ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // This gives every authenticated user the "ROLE_USER" permission.
        // Later, you can load specific roles from your database here.
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
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

    // --- FIX #2: Connect this to the user's verification status ---
    @Override
    public boolean isEnabled() {
        // This now correctly checks the 'is_enabled' column from your database.
        return user.isEnabled();
    }

    public boolean is2faEnabled() {
        return user.is2faEnabled();
    }

     public String getBio() {
        return user.getBio();
    }

    public String getMajor() {
        return user.getMajor();
    }

    public Integer getReputationScore() {
        return user.getReputationScore();
    }


}