package com.tuniv.backend.user.dto;

import com.tuniv.backend.auth.dto.VerificationInfo;

/**
 * Represents the detailed public profile of a user.
 *
 * @param userId            The unique identifier for the user.
 * @param username          The user's display name.
 * @param profilePhotoUrl   URL for the user's avatar.
 * @param bio               The user's self-description.
 * @param major             The user's declared major.
 * @param reputationScore   The user's reputation score.
 * @param topicsCount       ✅ Renamed from questionsCount. The total number of topics created by the user.
 * @param repliesCount      ✅ Renamed from answersCount. The total number of replies posted by the user.
 * @param followersCount    The total number of users following this user.
 * @param verification      ✅ Standardized name. DTO with primary university verification details.
 */
public record UserProfileDto(
    Integer userId,
    String username,
    String profilePhotoUrl,
    String bio,
    String major,
    Integer reputationScore,
    int topicsCount,
    int repliesCount,
    int followersCount,
    VerificationInfo verification
) {}