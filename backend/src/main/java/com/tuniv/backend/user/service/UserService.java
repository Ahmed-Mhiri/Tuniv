package com.tuniv.backend.user.service;

import com.tuniv.backend.community.dto.CommunitySummaryDto;
import com.tuniv.backend.community.mapper.CommunityMapper;
import com.tuniv.backend.community.model.CommunityMembership;
import com.tuniv.backend.community.repository.CommunityMembershipRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.ReplyResponseDto;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.dto.VoteInfo;
import com.tuniv.backend.qa.mapper.TopicMapper;
import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.qa.repository.ReplyRepository;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.university.repository.UniversityMembershipRepository;
import com.tuniv.backend.user.dto.LeaderboardUserDto;
import com.tuniv.backend.user.dto.UserProfileDto;
import com.tuniv.backend.user.dto.UserProfileUpdateRequest;
import com.tuniv.backend.user.mapper.UserMapper;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    // Repositories
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final ReplyRepository replyRepository;
    private final VoteRepository voteRepository;
    private final CommunityMembershipRepository communityMembershipRepository;
    private final UniversityMembershipRepository universityMembershipRepository;

    // Mappers
    private final UserMapper userMapper;
    private final TopicMapper topicMapper;
    private final CommunityMapper communityMapper;

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfileById(Integer userId) {
        return fetchUserProfile(userId);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile(UserDetailsImpl currentUser) {
        return fetchUserProfile(currentUser.getId());
    }

    @Transactional
    public UserProfileDto updateCurrentUserProfile(UserDetailsImpl currentUser, UserProfileUpdateRequest updateRequest) {
        User userToUpdate = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));

        // Use Optional.ofNullable to write cleaner update logic
        Optional.ofNullable(updateRequest.bio()).ifPresent(userToUpdate::setBio);
        Optional.ofNullable(updateRequest.major()).ifPresent(userToUpdate::setMajor);
        Optional.ofNullable(updateRequest.profilePhotoUrl()).ifPresent(userToUpdate::setProfilePhotoUrl);

        User updatedUser = userRepository.save(userToUpdate);

        // ✅ SIMPLIFIED: Fetch primary membership and pass the updated user to the mapper
        Optional<UniversityMembership> primaryMembership =
            universityMembershipRepository.findByUserIdAndIsPrimaryTrue(updatedUser.getUserId());

        return userMapper.toUserProfileDto(updatedUser, primaryMembership);
    }

    @Transactional(readOnly = true)
public List<CommunitySummaryDto> getUserCommunities(Integer userId) {
    // ✅ USE THE NEW EFFICIENT METHOD
    List<CommunityMembership> memberships = communityMembershipRepository.findWithCommunityByUserId(userId);
    return memberships.stream()
        .map(membership -> communityMapper.toSummaryDto(membership.getCommunity()))
        .collect(Collectors.toList());
}


    @Transactional(readOnly = true)
    public List<LeaderboardUserDto> getLeaderboardUsers() {
        return userRepository.findTop5ByOrderByReputationScoreDesc().stream()
            .map(userMapper::toLeaderboardUserDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TopicSummaryDto> getUserTopics(Integer userId, UserDetailsImpl currentUser) {
        // ... (This method's logic is complex but correct and unaffected by the User entity changes)
        // It correctly fetches topics and enriches them with vote data. No changes needed.
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        List<Topic> topics = topicRepository.findByAuthor_UserIdOrderByCreatedAtDesc(userId);
        return topicMapper.toTopicSummaryDtos(topics, currentUser);
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getUserReplies(Integer userId, UserDetailsImpl currentUser) {
        // ... (This method is also correct, as it focuses on Reply entities)
        // The helper method `convertRepliesToDtos` correctly handles DTO conversion.
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        List<Reply> allReplies = replyRepository.findByAuthor_UserIdOrderByCreatedAtDesc(userId);
        return topicMapper.toReplyResponseDtos(allReplies, currentUser);
    }


    /**
     * ✅ REFACTORED: Private helper method to fetch user profile data efficiently.
     * This reduces code duplication between getting a profile by ID and getting the current user's profile.
     */
    private UserProfileDto fetchUserProfile(Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // ✅ EFFICIENT: Fetch the primary membership in the same transaction
        Optional<UniversityMembership> primaryMembership =
            universityMembershipRepository.findByUserIdAndIsPrimaryTrue(userId);

        // ✅ CLEAN: Delegate all mapping logic to the mapper
        return userMapper.toUserProfileDto(user, primaryMembership);
    }
}