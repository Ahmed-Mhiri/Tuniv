package com.tuniv.backend.auth.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // <-- IMPORT ADDED
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder; // <-- IMPORT ADDED
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.auth.dto.ForgotPasswordRequest;
import com.tuniv.backend.auth.dto.JwtResponse;
import com.tuniv.backend.auth.dto.LoginRequest;
import com.tuniv.backend.auth.dto.RegisterRequest;
import com.tuniv.backend.auth.dto.ResetPasswordRequest;
import com.tuniv.backend.auth.mapper.AuthMapper;
import com.tuniv.backend.config.security.jwt.JwtUtil;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.service.AuthEmailService;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository; // <-- Make sure package is correct

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthEmailService notificationService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TwoFactorAuthService twoFactorAuthService;

     public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (!userDetails.isEnabled()) {
            throw new BadCredentialsException("User account is not yet verified. Please check your email.");
        }

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found after authentication"));

        if (user.is2faEnabled()) {
            if (loginRequest.code() == null || loginRequest.code().isEmpty()) {
                return AuthMapper.toJwtResponse(null, userDetails, true);
            }

            // --- TEMPORARY DEBUG LOGS FOR 2FA ---
            System.out.println("--- 2FA DEBUG ---");
            String code = loginRequest.code();
            boolean isValid = twoFactorAuthService.isOtpValid(user.getTwoFactorAuthSecret(), code);
            System.out.println("Received Code: " + code);
            System.out.println("Is Code Valid?: " + isValid);
            System.out.println("-----------------");
            // --- END DEBUG LOGS ---

            if (!isValid) {
                throw new BadCredentialsException("Invalid 2FA code");
            }
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateJwtToken(authentication);

        return AuthMapper.toJwtResponse(jwt, userDetails, false);
    }


    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.username())) {
            throw new IllegalArgumentException("Error: Username is already taken!");
        }
        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(registerRequest.username());
        user.setEmail(registerRequest.email());
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setReputationScore(0);
        user.set2faEnabled(false);
        user.setEnabled(false); // User is disabled until they verify their email

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);

        userRepository.save(user);

        // Send the verification email
        notificationService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public void verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid verification token."));

        user.setEnabled(true);
        user.setVerificationToken(null); // Invalidate the token after use
        userRepository.save(user);
    }
    
    // --- 2FA SETUP METHODS ---

    @Transactional
    public String generate2faSetup(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // Generate and save a new secret for the user
        String secret = twoFactorAuthService.generateNewSecret();
        user.setTwoFactorAuthSecret(secret);
        userRepository.save(user);

        // Return a Data URI for the QR code image
        return twoFactorAuthService.generateQrCodeImageUri(secret, user.getEmail());
    }

    @Transactional
    public void enable2fa(String username, String code) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Verify the code against the secret stored temporarily
        if (!twoFactorAuthService.isOtpValid(user.getTwoFactorAuthSecret(), code)) {
            throw new IllegalArgumentException("Verification code is incorrect.");
        }

        // If the code is correct, permanently enable 2FA
        user.set2faEnabled(true);
        userRepository.save(user);
    }
    
    // --- PASSWORD RESET METHODS ---
    
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Error: No user found with that email address."));

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1)); // Token valid 1 hour

        userRepository.save(user);

        notificationService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Error: Invalid token."));

        if (user.getResetPasswordTokenExpiry().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
    throw new IllegalArgumentException("Error: Token has expired.");
}

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetPasswordToken(null); // Invalidate the token
        user.setResetPasswordTokenExpiry(null);

        userRepository.save(user);
    }
     @Transactional
    public JwtResponse disable2fa(String username) { // <-- FIX: Return type is JwtResponse
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            
        user.set2faEnabled(false);
        user.setTwoFactorAuthSecret(null);
        userRepository.save(user);

        // Re-create UserDetails to reflect the change
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        
        // Return the updated state in a JwtResponse object
        // The token can be null because the frontend only needs the updated flags.
        return AuthMapper.toJwtResponse(null, userDetails, false);
    }
    
}