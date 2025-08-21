package com.tuniv.backend.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.dto.UserProfileDto;
import com.tuniv.backend.user.dto.UserProfileUpdateRequest;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Fetches a user's public profile by their ID.
     */
    public UserProfileDto getUserProfileById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapUserToProfileDto(user);
    }

    /**
     * Fetches the profile of the currently authenticated user.
     */
    public UserProfileDto getCurrentUserProfile(UserDetailsImpl currentUser) {
        // We already know the user exists because they are authenticated.
        User user = userRepository.findById(currentUser.getId()).get();
        return mapUserToProfileDto(user);
    }

    /**
     * Updates the profile of the currently authenticated user.
     */
    @Transactional
    public UserProfileDto updateCurrentUserProfile(UserDetailsImpl currentUser, UserProfileUpdateRequest updateRequest) {
        User userToUpdate = userRepository.findById(currentUser.getId()).get();

        // Update fields if they are provided in the request
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
        return mapUserToProfileDto(updatedUser);
    }

    // Helper method to map a User entity to a public DTO
    private UserProfileDto mapUserToProfileDto(User user) {
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