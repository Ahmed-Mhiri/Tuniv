package com.tuniv.backend.user.mapper;

import org.springframework.stereotype.Component;

import com.tuniv.backend.university.model.VerificationStatus;
import com.tuniv.backend.user.dto.LeaderboardUserDto;
import com.tuniv.backend.user.dto.UserProfileDto;
import com.tuniv.backend.user.dto.UserSummaryDto;
import com.tuniv.backend.user.dto.VerificationInfo;
import com.tuniv.backend.user.model.User;

@Component
public class UserMapper {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private UserMapper() {}

    /**
     * Maps a User entity to a lightweight summary DTO, used for lists and brief mentions.
     */
    public static UserSummaryDto toUserSummaryDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserSummaryDto(
            user.getUserId(),
            user.getUsername(),
            user.getProfilePhotoUrl(),
            user.getReputationScore()
        );
    }

    /**
     * Maps a User entity to a detailed profile DTO, including stats and verification status.
     */
    public static UserProfileDto toUserProfileDto(User user, long questionsCount, long answersCount, long followersCount) {
        if (user == null) {
            return null;
        }
    
        // Find the first verified membership for the user to display their primary affiliation.
        VerificationInfo verificationInfo = user.getMemberships().stream()
                .filter(m -> m.getStatus() == VerificationStatus.VERIFIED)
                .findFirst()
                .map(verifiedMembership -> new VerificationInfo(
                    verifiedMembership.getUniversity().getName(), 
                    verifiedMembership.getRole()
                ))
                .orElse(null); // It will be null if the user is not verified at any university.

        return new UserProfileDto(
            user.getUserId(),
            user.getUsername(),
            user.getProfilePhotoUrl(),
            user.getBio(),
            user.getMajor(),
            user.getReputationScore(),
            questionsCount,
            answersCount,
            followersCount,
            verificationInfo // This will be null for unverified users.
        );
    }

    /**
     * Maps a User entity to a specialized DTO for the leaderboard.
     */
    public static LeaderboardUserDto toLeaderboardUserDto(User user) {
        if (user == null) {
            return null;
        }

        return new LeaderboardUserDto(
            user.getUserId(),
            user.getUsername(),
            user.getReputationScore(),
            user.getProfilePhotoUrl()
        );
    }
}