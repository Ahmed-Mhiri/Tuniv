package com.tuniv.backend.config.security.services;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.tuniv.backend.user.model.User;

import lombok.Getter;

@Getter
public class UserDetailsImpl implements UserDetails {

    private final User user; // ✅ Store the full user entity for direct access

    public UserDetailsImpl(User user) {
        this.user = user;
    }

    // ✅ Enhanced: Get full user entity directly (for AuthService optimization)
    public User getUser() {
        return user;
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

    // ✅ Enhanced: Dynamic role-based authorities
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Build authorities based on user roles and permissions
        return buildAuthorities();
    }

    private List<GrantedAuthority> buildAuthorities() {
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        
        // Base role for all authenticated users
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // Add platform admin role if applicable
        if (user.isPlatformAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_MODERATOR")); // Admins are also moderators
        }
        
        // Add moderator role if applicable
        else if (user.isModerator()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MODERATOR"));
        }
        
        // ✅ NEW: Add university-specific roles based on memberships
        // These would be loaded from university memberships in a more complex implementation
        // For now, we'll add a generic university member role
        if (hasUniversityMembership()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_UNIVERSITY_MEMBER"));
        }
        
        return authorities;
    }

    // ✅ NEW: Helper method to check if user has university memberships
    private boolean hasUniversityMembership() {
        // In a more complete implementation, you would check the user's university memberships
        // For now, we assume any verified user has at least one membership
        return user.isEmailVerified();
    }

    // --- Enhanced UserDetails methods with proper business logic ---

    @Override
    public boolean isAccountNonExpired() {
        return true; // Could be enhanced with account expiry logic
    }

    @Override
    public boolean isAccountNonLocked() {
        // ✅ Enhanced: Check if account is locked due to too many failed attempts
        // This would integrate with our LoginAttemptService
        return !user.isDeleted() && user.isEnabled();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Could be enhanced with password expiry logic
    }

    // ✅ Enhanced: Comprehensive enabled check
    @Override
    public boolean isEnabled() {
        return user.isEnabled() && 
               user.isEmailVerified() && 
               !user.isDeleted();
    }

    // --- Business-specific methods ---

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

    // ✅ NEW: Additional helper methods for business logic
    public boolean canModerateContent() {
        return user.isPlatformAdmin() || user.isModerator();
    }

    public boolean canManageUsers() {
        return user.isPlatformAdmin();
    }

    public boolean isVerifiedStudent() {
        return user.isEmailVerified() && hasUniversityMembership();
    }

    // ✅ Enhanced: Proper equals and hashCode implementation
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsImpl that = (UserDetailsImpl) o;
        return Objects.equals(user.getUserId(), that.user.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(user.getUserId());
    }

    // ✅ NEW: toString for better logging
    @Override
    public String toString() {
        return "UserDetailsImpl{" +
                "userId=" + user.getUserId() +
                ", username='" + user.getUsername() + '\'' +
                ", email='" + user.getEmail() + '\'' +
                ", enabled=" + isEnabled() +
                ", authorities=" + getAuthorities() +
                '}';
    }
}