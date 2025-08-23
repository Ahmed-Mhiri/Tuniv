package com.tuniv.backend.auth.mapper;

import com.tuniv.backend.auth.dto.JwtResponse;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

public class AuthMapper {

    // A static method for simple, stateless mapping.
    public static JwtResponse toJwtResponse(String jwt, UserDetailsImpl userDetails) {
        return new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail()
        );
    }
}