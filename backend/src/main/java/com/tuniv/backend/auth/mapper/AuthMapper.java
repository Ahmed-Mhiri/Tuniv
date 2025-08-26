package com.tuniv.backend.auth.mapper;

import com.tuniv.backend.auth.dto.JwtResponse;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

public class AuthMapper {
    public static JwtResponse toJwtResponse(String jwt, UserDetailsImpl userDetails, boolean is2faRequired) {
        return new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getProfilePhotoUrl(),
                is2faRequired // <-- ADD THIS
        );
    }
}