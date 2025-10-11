package com.tuniv.backend.auth.mapper;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tuniv.backend.auth.dto.JwtResponse;
import com.tuniv.backend.auth.dto.VerificationInfo;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.user.model.User;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AuthMapper {

    /**
     * Creates a JwtResponse DTO from a User entity and other authentication details.
     */
    public JwtResponse toJwtResponse(
            String token,
            User user,
            Optional<UniversityMembership> primaryMembership,
            boolean is2faRequired) {
        
        // Validate input parameters
        Objects.requireNonNull(user, "User cannot be null");
        Objects.requireNonNull(primaryMembership, "Primary membership optional cannot be null");
        
        log.debug("Mapping user {} to JwtResponse. 2FA required: {}", user.getUsername(), is2faRequired);
        
        // Create VerificationInfo DTO from the primary membership, or null if not present
        VerificationInfo verificationInfo = primaryMembership
                .map(membership -> {
                    if (membership.getUniversity() == null) {
                        log.warn("University membership {} has null university", membership.getId());
                        return null;
                    }
                    return new VerificationInfo(
                            membership.getUniversity().getName(),
                            membership.getStatus(),
                            membership.getRole()
                    );
                })
                .orElse(null);

        // Map all fields from the User entity to the JwtResponse
        return new JwtResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfilePhotoUrl(),
                user.getBio(),
                user.getMajor(),
                user.getReputationScore(),
                verificationInfo,
                user.getTopicCount(),
                user.getReplyCount(),
                user.getFollowerCount(),
                user.getFollowingCount(),
                0L, // Placeholder for unreadNotificationsCount
                0L, // Placeholder for unreadMessagesCount
                is2faRequired,
                user.is2faEnabled()
        );
    }
    
    /**
     * Creates a UserProfile DTO for non-token responses (like after disabling 2FA)
     */
    public UserProfileDto toUserProfileDto(User user, Optional<UniversityMembership> primaryMembership) {
        Objects.requireNonNull(user, "User cannot be null");
        
        VerificationInfo verificationInfo = primaryMembership
                .map(membership -> new VerificationInfo(
                        membership.getUniversity().getName(),
                        membership.getStatus(),
                        membership.getRole()
                ))
                .orElse(null);
        
        return new UserProfileDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfilePhotoUrl(),
                user.getBio(),
                user.getMajor(),
                user.getReputationScore(),
                verificationInfo,
                user.getTopicCount(),
                user.getReplyCount(),
                user.getFollowerCount(),
                user.getFollowingCount(),
                user.is2faEnabled()
        );
    }
    
    // UserProfile DTO for non-authentication contexts
    public record UserProfileDto(
            Integer id,
            String username,
            String email,
            String profilePhotoUrl,
            String bio,
            String major,
            int reputationScore,
            VerificationInfo verificationInfo,
            int topicsCount,
            int repliesCount,
            int followersCount,
            int followingCount,
            boolean is2faEnabled
    ) {}
}