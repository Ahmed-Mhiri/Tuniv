package com.tuniv.backend.config.security.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // ✅ Enhanced: Read-only for better performance
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User Not Found with username: " + username);
                });

        // ✅ NEW: Additional validation checks
        validateUserAccount(user, username);
        
        log.debug("Successfully loaded user: {} with ID: {}", username, user.getUserId());
        
        return new UserDetailsImpl(user);
    }

    // ✅ NEW: Load user by email for additional flexibility
    @Transactional(readOnly = true)
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User Not Found with email: " + email);
                });

        validateUserAccount(user, email);
        
        log.debug("Successfully loaded user by email: {} with ID: {}", email, user.getUserId());
        
        return new UserDetailsImpl(user);
    }

    // ✅ NEW: Comprehensive user account validation
    private void validateUserAccount(User user, String identifier) {
        // Check if account is deleted
        if (user.isDeleted()) {
            log.warn("Attempt to load deleted account: {}", identifier);
            throw new UsernameNotFoundException("Account has been deleted: " + identifier);
        }

        // Check if account is disabled
        if (!user.isEnabled()) {
            log.warn("Attempt to load disabled account: {}", identifier);
            throw new UsernameNotFoundException("Account is disabled: " + identifier);
        }

        // ✅ NEW: Check email verification (if required for your security policy)
        // Note: We might allow login before email verification for some flows
        // if (!user.isEmailVerified()) {
        //     log.warn("Attempt to load unverified account: {}", identifier);
        //     throw new UsernameNotFoundException("Account email not verified: " + identifier);
        // }
    }

    // ✅ NEW: Method to check if user exists (for validation purposes)
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    // ✅ NEW: Method to check if email exists (for validation purposes)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}