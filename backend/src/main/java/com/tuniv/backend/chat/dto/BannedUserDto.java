package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class BannedUserDto {
    private Integer userId;
    private String username;
    private String profilePhotoUrl;
    private String banReason;
    private Instant bannedAt;
    private Integer bannedByUserId;
    private String bannedByUsername;
}