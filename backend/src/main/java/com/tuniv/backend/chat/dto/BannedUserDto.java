// BannedUserDto.java
package com.tuniv.backend.chat.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BannedUserDto {
    private Integer userId;
    private String username;
    private String profilePhotoUrl;
    private String banReason;
    private Instant bannedAt;
    private Integer bannedByUserId;
    private String bannedByUsername;
}