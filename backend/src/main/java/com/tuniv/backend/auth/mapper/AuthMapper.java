package com.tuniv.backend.auth.mapper;

import com.tuniv.backend.auth.dto.JwtResponse;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

public class AuthMapper {
    public static JwtResponse toJwtResponse(String jwt, UserDetailsImpl userDetails, 
                                            long questionsCount, long answersCount, long followersCount,
                                            boolean is2faRequired) {
        return new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getProfilePhotoUrl(),
                userDetails.getBio(),
                userDetails.getMajor(),
                userDetails.getReputationScore(),
                // --- PASS NEW VALUES ---
                questionsCount,
                answersCount,
                followersCount,
                // --- 2FA ---
                is2faRequired,
                userDetails.is2faEnabled()
        );
    }
}