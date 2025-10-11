package com.tuniv.backend.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BanMemberRequest(
    @NotBlank @Size(max = 500)
    String reason,
    
    // Optional: for temporary bans. If null, the ban is permanent.
    Long durationInHours 
) {}