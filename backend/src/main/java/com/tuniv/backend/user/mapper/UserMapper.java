package com.tuniv.backend.user.mapper;

import com.tuniv.backend.user.dto.UserProfileDto;
import com.tuniv.backend.user.model.User;

public class UserMapper {

    public static UserProfileDto toUserProfileDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserProfileDto(
                user.getUserId(),
                user.getUsername(),
                user.getProfilePhotoUrl(),
                user.getBio(),
                user.getMajor(),
                user.getReputationScore()
        );
    }
}