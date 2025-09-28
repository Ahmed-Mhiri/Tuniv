package com.tuniv.backend.follow.service;
import java.util.List;
import java.util.stream.Collectors;

import com.tuniv.backend.community.repository.CommunityRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.follow.dto.FollowRequestDto;
import com.tuniv.backend.follow.model.Follow;
import com.tuniv.backend.follow.repository.FollowRepository;
import com.tuniv.backend.notification.event.NewFollowerEvent;
import com.tuniv.backend.qa.repository.TagRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.follow.dto.FollowSummaryDto;
import com.tuniv.backend.follow.dto.FollowerInfoDto;
import com.tuniv.backend.follow.model.FollowableType;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.user.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final ModuleRepository moduleRepository;
    private final TagRepository tagRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher; // ✅ Added


   @Transactional
    public void follow(FollowRequestDto request, UserDetailsImpl currentUserDetails) {
        User follower = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        // 1. Validate that the target being followed actually exists.
        validateTargetExists(request.targetType(), request.targetId());
        
        // 2. Prevent users from following themselves.
        if (request.targetType() == FollowableType.USER && follower.getUserId().equals(request.targetId())) {
            throw new IllegalArgumentException("You cannot follow yourself.");
        }
        
        // 3. Check if the user is already following the target to prevent duplicates.
        boolean alreadyFollowing = followRepository.existsByUser_UserIdAndTargetTypeAndTargetId(
            follower.getUserId(), request.targetType(), request.targetId()
        );

        if (!alreadyFollowing) {
            Follow follow = new Follow(follower, request.targetType(), request.targetId());
            followRepository.save(follow);

            // ✅ Publish event for user follows
            if (request.targetType() == FollowableType.USER) {
                User followedUser = userRepository.findById(request.targetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Followed user not found"));
                eventPublisher.publishEvent(new NewFollowerEvent(follower, followedUser));
            }
        }
    }


    @Transactional
    public void unfollow(FollowRequestDto request, UserDetailsImpl currentUserDetails) {
        // Find the specific follow relationship and delete it if it exists.
        followRepository.findByUser_UserIdAndTargetTypeAndTargetId(
            currentUserDetails.getId(), request.targetType(), request.targetId()
        ).ifPresent(followRepository::delete);
    }
    
    /**
     * Helper method to ensure a target (Community, User, etc.) exists before following.
     */
    private void validateTargetExists(FollowableType type, Integer id) {
        boolean exists = switch (type) {
            case USER -> userRepository.existsById(id);
            case COMMUNITY -> communityRepository.existsById(id);
            case MODULE -> moduleRepository.existsById(id);
            case TAG -> tagRepository.existsById(id);
        };
        
        if (!exists) {
            throw new ResourceNotFoundException("Cannot follow. The " + type.name().toLowerCase() + " with ID " + id + " does not exist.");
        }
    }
    
    /**
     * Get all things a user follows
     */
    @Transactional(readOnly = true)
    public List<FollowSummaryDto> getFollowing(Integer userId) {
        List<Follow> follows = followRepository.findAllByUser_UserId(userId);
        
        return follows.stream()
                .map(this::toFollowSummaryDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all things the current user follows
     */
    @Transactional(readOnly = true)
    public List<FollowSummaryDto> getCurrentUserFollowing(UserDetailsImpl currentUser) {
        return getFollowing(currentUser.getId());
    }
    
    /**
     * Get followers for a specific target (user, community, etc.)
     */
    @Transactional(readOnly = true)
    public List<FollowerInfoDto> getFollowers(FollowableType targetType, Integer targetId) {
        List<Follow> followers = followRepository.findByTargetTypeAndTargetId(targetType, targetId);
        
        return followers.stream()
                .map(follow -> new FollowerInfoDto(
                    userMapper.toUserSummaryDto(follow.getUser()),
                    follow.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * Get follower count for a specific target
     */
    @Transactional(readOnly = true)
    public long getFollowerCount(FollowableType targetType, Integer targetId) {
        return followRepository.countByTargetTypeAndTargetId(targetType, targetId);
    }
    
    /**
     * Check if current user follows a specific target
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(UserDetailsImpl currentUser, FollowableType targetType, Integer targetId) {
        return followRepository.existsByUser_UserIdAndTargetTypeAndTargetId(
            currentUser.getId(), targetType, targetId
        );
    }
    
    /**
     * Get follow relationship details if it exists
     */
    @Transactional(readOnly = true)
    public FollowSummaryDto getFollowRelationship(UserDetailsImpl currentUser, FollowableType targetType, Integer targetId) {
        return followRepository.findByUser_UserIdAndTargetTypeAndTargetId(
            currentUser.getId(), targetType, targetId
        ).map(this::toFollowSummaryDto)
         .orElse(null);
    }
    
    /**
     * Convert Follow entity to FollowSummaryDto
     */
    private FollowSummaryDto toFollowSummaryDto(Follow follow) {
        String targetName = getTargetName(follow.getTargetType(), follow.getTargetId());
        
        return new FollowSummaryDto(
            follow.getFollowId(),
            follow.getTargetType(),
            follow.getTargetId(),
            targetName,
            follow.getCreatedAt()
        );
    }
    
    /**
     * Get the name of the target being followed
     */
    private String getTargetName(FollowableType targetType, Integer targetId) {
        return switch (targetType) {
            case USER -> userRepository.findById(targetId)
                    .map(User::getUsername)
                    .orElse("Unknown User");
            case COMMUNITY -> communityRepository.findById(targetId)
                    .map(com.tuniv.backend.community.model.Community::getName)
                    .orElse("Unknown Community");
            case MODULE -> moduleRepository.findById(targetId)
                    .map(com.tuniv.backend.university.model.Module::getName)
                    .orElse("Unknown Module");
            case TAG -> tagRepository.findById(targetId)
                    .map(com.tuniv.backend.qa.model.Tag::getName)
                    .orElse("Unknown Tag");
        };
    }
}