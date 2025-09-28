package com.tuniv.backend.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.community.dto.CommunitySummaryDto;
import com.tuniv.backend.community.mapper.CommunityMapper;
import com.tuniv.backend.community.model.CommunityMembership;
import com.tuniv.backend.community.repository.CommunityMembershipRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.follow.model.FollowableType;
import com.tuniv.backend.follow.repository.FollowRepository;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.QuestionRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.dto.LeaderboardUserDto;
import com.tuniv.backend.user.dto.UserProfileDto;
import com.tuniv.backend.user.dto.UserProfileUpdateRequest;
import com.tuniv.backend.user.mapper.UserMapper;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FollowRepository followRepository; // ✅ REPLACED FollowService
    private final CommunityMembershipRepository communityMembershipRepository; // ✅ NEW
    private final CommunityMapper communityMapper; // ✅ Inject CommunityMapper


    @Transactional(readOnly = true)
    public UserProfileDto getUserProfileById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        long questionsCount = questionRepository.countByAuthor_UserId(userId);
        long answersCount = answerRepository.countByAuthor_UserId(userId);
        // ✅ UPDATED: Use the new repository to count followers for a USER target
        long followersCount = followRepository.countByTargetTypeAndTargetId(FollowableType.USER, userId);

        return UserMapper.toUserProfileDto(user, questionsCount, answersCount, followersCount);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile(UserDetailsImpl currentUser) {
        Integer userId = currentUser.getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        long questionsCount = questionRepository.countByAuthor_UserId(userId);
        long answersCount = answerRepository.countByAuthor_UserId(userId);
        // ✅ UPDATED: Use the new repository to count followers for a USER target
        long followersCount = followRepository.countByTargetTypeAndTargetId(FollowableType.USER, userId);
        
        return UserMapper.toUserProfileDto(user, questionsCount, answersCount, followersCount);
    }

    @Transactional
    public UserProfileDto updateCurrentUserProfile(UserDetailsImpl currentUser, UserProfileUpdateRequest updateRequest) {
        User userToUpdate = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));

        if (updateRequest.bio() != null) {
            userToUpdate.setBio(updateRequest.bio());
        }
        if (updateRequest.major() != null) {
            userToUpdate.setMajor(updateRequest.major());
        }
        if (updateRequest.profilePhotoUrl() != null) {
            userToUpdate.setProfilePhotoUrl(updateRequest.profilePhotoUrl());
        }

        User updatedUser = userRepository.save(userToUpdate);

        long questionsCount = questionRepository.countByAuthor_UserId(updatedUser.getUserId());
        long answersCount = answerRepository.countByAuthor_UserId(updatedUser.getUserId());
        // ✅ UPDATED: Use the new repository to count followers for a USER target
        long followersCount = followRepository.countByTargetTypeAndTargetId(FollowableType.USER, updatedUser.getUserId());

        return UserMapper.toUserProfileDto(updatedUser, questionsCount, answersCount, followersCount);
    }

    @Transactional(readOnly = true)
public List<CommunitySummaryDto> getUserCommunities(UserDetailsImpl currentUser) {
    // 1. Fetch all community memberships for the user. This part is correct.
    List<CommunityMembership> memberships = communityMembershipRepository.findByUser_UserId(currentUser.getId());
    
    // 2. Use the CommunityMapper to convert each community into its summary DTO.
   return memberships.stream()
                .map(membership -> communityMapper.toSummaryDto(membership.getCommunity())) // ✅ Use instance method
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardUserDto> getLeaderboardUsers() {
        return userRepository.findTop5ByOrderByReputationScoreDesc().stream()
                .map(UserMapper::toLeaderboardUserDto)
                .collect(Collectors.toList());
    }
}