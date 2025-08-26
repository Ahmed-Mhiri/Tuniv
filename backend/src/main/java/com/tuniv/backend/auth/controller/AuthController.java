package com.tuniv.backend.auth.controller;

import com.tuniv.backend.auth.dto.*; // Assuming a new DTO is created here
import com.tuniv.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException; // <-- IMPORT THIS
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

// A simple, type-safe DTO for the 2FA enable request
record Enable2faRequest(String code) {}

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse jwtResponse = authService.login(loginRequest);
            return ResponseEntity.ok(jwtResponse);
        } catch (BadCredentialsException e) { // <-- BE SPECIFIC
            // This now only catches authentication failures
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            authService.register(registerRequest);
            // --- FIX: Return a JSON object ---
            return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request);
            // --- FIX: Return a JSON object ---
            return ResponseEntity.ok(Map.of("message", "A password reset link has been sent to your email."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            // --- FIX: Return a JSON object ---
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/2fa/generate")
    public ResponseEntity<?> generate2fa(Principal principal) {
        String qrCodeUri = authService.generate2faSetup(principal.getName());
        // This was already correct, returning a JSON object.
        return ResponseEntity.ok(Map.of("qrCodeUri", qrCodeUri));
    }

    @PostMapping("/2fa/enable")
    // --- REFINEMENT: Use a DTO instead of a Map ---
    public ResponseEntity<?> enable2fa(Principal principal, @RequestBody Enable2faRequest payload) {
        try {
            authService.enable2fa(principal.getName(), payload.code());
            // --- FIX: Return a JSON object ---
            return ResponseEntity.ok(Map.of("message", "2FA enabled successfully!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestParam("token") String token) {
        try {
            authService.verifyUser(token);
            return ResponseEntity.ok(Map.of("message", "Account verified successfully!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/2fa/disable")
    public ResponseEntity<?> disable2fa(Principal principal) {
        // --- FIX: This now correctly returns a JwtResponse ---
        JwtResponse updatedResponse = authService.disable2fa(principal.getName());
        return ResponseEntity.ok(updatedResponse);
    }
}