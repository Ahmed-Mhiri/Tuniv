package com.tuniv.backend.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder; // <-- IMPORT ADDED
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tuniv.backend.auth.dto.JwtResponse;
import com.tuniv.backend.auth.dto.LoginRequest;
import com.tuniv.backend.auth.dto.RegisterRequest;
import com.tuniv.backend.auth.mapper.AuthMapper;
import com.tuniv.backend.config.security.jwt.JwtUtil;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // --- THIS IS THE CHANGE ---
        // Use the dedicated mapper instead of creating the DTO here.
        return AuthMapper.toJwtResponse(jwt, userDetails);
    }

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

        userRepository.save(user);
    }
}