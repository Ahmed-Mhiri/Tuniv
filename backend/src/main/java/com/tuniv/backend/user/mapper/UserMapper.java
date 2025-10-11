package com.tuniv.backend.user.mapper;

import com.tuniv.backend.auth.dto.VerificationInfo;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.user.dto.LeaderboardUserDto;
import com.tuniv.backend.user.dto.UserProfileDto;
import com.tuniv.backend.user.dto.UserSummaryDto;
import com.tuniv.backend.user.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserMapper {

    /**
     * Maps a User entity to a lightweight summary DTO.
     */
    public UserSummaryDto toUserSummaryDto(User user) {
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
     * ✅ UPDATED: Maps a User entity and their optional primary membership to a detailed profile DTO.
     * This method now leverages the denormalized counts directly from the User object for efficiency.
     *
     * @param user              The User entity to map.
     * @param primaryMembership An Optional containing the user's primary university membership.
     * @return A detailed UserProfileDto.
     */
    public UserProfileDto toUserProfileDto(User user, Optional<UniversityMembership> primaryMembership) {
        if (user == null) {
            return null;
        }

        // Map the optional membership to a VerificationInfo DTO
        VerificationInfo verificationInfo = primaryMembership
        .map(membership -> new VerificationInfo(
                membership.getUniversity().getName(),
                membership.getStatus(), // ✅ ADD THIS
                membership.getRole()
        ))
        .orElse(null);


        return new UserProfileDto(
                user.getUserId(),
                user.getUsername(),
                user.getProfilePhotoUrl(),
                user.getBio(),
                user.getMajor(),
                user.getReputationScore(),
                user.getTopicCount(),      // ✅ Use denormalized count
                user.getReplyCount(),      // ✅ Use denormalized count
                user.getFollowerCount(),   // ✅ Use denormalized count
                verificationInfo
        );
    }

    /**
     * Maps a User entity to a specialized DTO for the leaderboard.
     */
    public LeaderboardUserDto toLeaderboardUserDto(User user) {
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