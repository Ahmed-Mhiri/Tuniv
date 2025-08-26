package com.tuniv.backend.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
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

}