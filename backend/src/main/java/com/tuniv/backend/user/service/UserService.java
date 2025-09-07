package com.tuniv.backend.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.dto.CommunityDto;
import com.tuniv.backend.user.dto.LeaderboardUserDto;
import com.tuniv.backend.user.dto.UserProfileDto; // <-- IMPORT ADDED
import com.tuniv.backend.user.dto.UserProfileUpdateRequest;
import com.tuniv.backend.user.mapper.UserMapper;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileDto getUserProfileById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return UserMapper.toUserProfileDto(user); // Use the central mapper
    }

    public UserProfileDto getCurrentUserProfile(UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));
        return UserMapper.toUserProfileDto(user); // Use the central mapper
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
        return UserMapper.toUserProfileDto(updatedUser); // Use the central mapper
    }

    @Transactional(readOnly = true)
    public List<CommunityDto> getUserCommunities(UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));

        return user.getMemberships().stream()
            .map(membership -> {
                var university = membership.getUniversity();
                // Note: university.getMembers().size() might trigger extra queries.
                // For high-performance needs, this could be optimized later.
                return new CommunityDto(
                    university.getUniversityId(),
                    "UNIVERSITY",
                    university.getName(),
                    university.getMemberships() != null ? university.getMemberships().size() : 0
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * NEW: Retrieves the top 5 users for the leaderboard.
     */
    @Transactional(readOnly = true)
    public List<LeaderboardUserDto> getLeaderboardUsers() {
        return userRepository.findTop5ByOrderByReputationScoreDesc().stream()
            .map(UserMapper::toLeaderboardUserDto) 
            .collect(Collectors.toList());
    }

}