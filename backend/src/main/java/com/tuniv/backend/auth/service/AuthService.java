package com.tuniv.backend.auth.service;

import com.tuniv.backend.auth.dto.ForgotPasswordRequest;
import com.tuniv.backend.auth.dto.JwtResponse;
import com.tuniv.backend.auth.dto.LoginRequest;
import com.tuniv.backend.auth.dto.RegisterRequest;
import com.tuniv.backend.auth.dto.ResetPasswordRequest;
import com.tuniv.backend.auth.mapper.AuthMapper;
import com.tuniv.backend.config.security.jwt.JwtUtil;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.service.AuthEmailService;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.university.repository.UniversityMembershipRepository; // ✅ ADDED IMPORT
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.model.UserSettings; // ✅ ADDED IMPORT
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant; // ✅ CHANGED
import java.time.temporal.ChronoUnit; // ✅ ADDED
import java.util.Optional;
import java.util.UUID;

import com.tuniv.backend.auth.validation.PasswordValidator;
import com.tuniv.backend.shared.exception.auth.AccountLockedException;
import com.tuniv.backend.shared.exception.auth.AccountNotVerifiedException;
import com.tuniv.backend.shared.exception.auth.EmailAlreadyExistsException;
import com.tuniv.backend.shared.exception.auth.InvalidTwoFactorCodeException;
import com.tuniv.backend.shared.exception.auth.InvalidVerificationTokenException;
import com.tuniv.backend.shared.exception.auth.UsernameAlreadyExistsException;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthEmailService notificationService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UniversityMembershipRepository universityMembershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TwoFactorAuthService twoFactorAuthService;
    private final AuthMapper authMapper;
    private final LoginAttemptService loginAttemptService;
    private final PasswordValidator passwordValidator;

    @Transactional(readOnly = true)
    public JwtResponse login(LoginRequest loginRequest) {
        final String username = loginRequest.username();
        
        // Check rate limiting
        if (loginAttemptService.isBlocked(username)) {
            int remainingTime = 15; // minutes
            throw new AccountLockedException(
                "Account temporarily locked due to too many failed attempts. " +
                "Please try again in " + remainingTime + " minutes."
            );
        }

        try {
            log.info("Attempting login for user: {}", username);
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.password()));

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User user = userDetails.getUser(); // ✅ No additional database call

            if (!user.isEmailVerified()) {
                throw new AccountNotVerifiedException("User account is not yet verified. Please check your email.");
            }

            if (!user.isEnabled()) {
                throw new AccountLockedException("User account is disabled. Please contact support.");
            }

            // Handle 2FA Challenge
            if (user.is2faEnabled()) {
                if (loginRequest.code() == null || loginRequest.code().isEmpty()) {
                    log.info("2FA required for user: {}", username);
                    return authMapper.toJwtResponse(null, user, Optional.empty(), true);
                }
                if (!twoFactorAuthService.isOtpValid(user.getTwoFactorAuthSecret(), loginRequest.code())) {
                    throw new InvalidTwoFactorCodeException("Invalid 2FA code");
                }
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtil.generateJwtToken(authentication);

            // Fetch primary university membership
            Optional<UniversityMembership> primaryMembership =
                    universityMembershipRepository.findByUserIdAndIsPrimaryTrue(user.getUserId());

            loginAttemptService.loginSuccess(username);
            log.info("User {} logged in successfully", username);

            return authMapper.toJwtResponse(jwt, user, primaryMembership, false);

        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(username);
            int remainingAttempts = loginAttemptService.getRemainingAttempts(username);
            log.warn("Failed login attempt for user: {}. Remaining attempts: {}", username, remainingAttempts);
            throw new BadCredentialsException("Invalid username or password. Remaining attempts: " + remainingAttempts);
        }
    }

    @Transactional
    public void register(RegisterRequest registerRequest) {
        log.info("Processing registration for user: {}", registerRequest.username());
        
        if (userRepository.existsByUsername(registerRequest.username())) {
            throw new UsernameAlreadyExistsException("Error: Username is already taken!");
        }
        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new EmailAlreadyExistsException("Error: Email is already in use!");
        }

        // Validate password strength
        passwordValidator.validate(registerRequest.password());

        User user = new User(
                registerRequest.username(),
                registerRequest.email(),
                passwordEncoder.encode(registerRequest.password())
        );

        // Create and associate default user settings
        UserSettings settings = new UserSettings(user);
        user.setSettings(settings);

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);

        userRepository.save(user);
        log.info("New user registered successfully: {}", user.getUsername());

        notificationService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public void verifyUser(String token) {
        log.info("Verifying user with token: {}", token);
        
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new InvalidVerificationTokenException("Invalid verification token."));

        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        
        log.info("User {} verified successfully", user.getUsername());
    }

    @Transactional
    public String generate2faSetup(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String secret = twoFactorAuthService.generateNewSecret();
        user.setTwoFactorAuthSecret(secret);
        userRepository.save(user);

        log.info("Generated 2FA secret for user: {}", username);
        
        return twoFactorAuthService.generateQrCodeImageUri(secret, user.getEmail());
    }

    @Transactional
public void enable2fa(String username, String code) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    // Use the detailed validation for better error reporting
    TwoFactorAuthService.ValidationResult validationResult = 
        twoFactorAuthService.validateOtpWithDetails(user.getTwoFactorAuthSecret(), code);
    
    if (!validationResult.isValid()) {
        log.warn("2FA enablement failed for user: {}. Reason: {}", username, validationResult.getMessage());
        throw new InvalidTwoFactorCodeException("Verification code is incorrect: " + validationResult.getMessage());
    }

    user.set2faEnabled(true);
    userRepository.save(user);
    
    log.info("2FA enabled successfully for user: {} (time bucket: {}, offset: {})", 
             username, validationResult.getMatchedBucket(), validationResult.getTimeOffset());
}
    @Transactional
    public AuthMapper.UserProfileDto disable2fa(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        user.set2faEnabled(false);
        user.setTwoFactorAuthSecret(null);
        User savedUser = userRepository.save(user);

        // Fetch primary university membership
        Optional<UniversityMembership> primaryMembership =
                universityMembershipRepository.findByUserIdAndIsPrimaryTrue(savedUser.getUserId());

        log.info("2FA disabled for user: {}", username);
        
        return authMapper.toUserProfileDto(savedUser, primaryMembership);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Error: No user found with that email address."));

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));

        userRepository.save(user);
        
        log.info("Password reset token generated for user: {}", user.getUsername());
        
        notificationService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Error: Invalid token."));

        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Error: Token has expired.");
        }

        // Validate new password strength
        passwordValidator.validate(request.newPassword());

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);

        userRepository.save(user);
        
        log.info("Password reset successfully for user: {}", user.getUsername());
    }

    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int deletedCount = userRepository.deleteExpiredVerificationTokens(now);
        int resetCount = userRepository.deleteExpiredPasswordResetTokens(now);
        
        log.info("Cleaned up {} expired verification tokens and {} expired password reset tokens", 
                 deletedCount, resetCount);
    }
}