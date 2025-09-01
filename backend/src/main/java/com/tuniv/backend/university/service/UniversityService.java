package com.tuniv.backend.university.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.UserJoinedUniversityEvent;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.dto.UniversityDto; // <-- IMPORT ADDED
import com.tuniv.backend.university.mapper.UniversityMapper;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.university.repository.UniversityMembershipRepository;
import com.tuniv.backend.university.repository.UniversityRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UniversityService {

    private final UniversityRepository universityRepository;
    // --- ModuleRepository is no longer needed here ---
    private final UserRepository userRepository;
    private final UniversityMembershipRepository membershipRepository;
        private final ApplicationEventPublisher eventPublisher;


    @Transactional(readOnly = true)
    @Cacheable("universities")
    public List<UniversityDto> getAllUniversities(UserDetailsImpl currentUserDetails) {
        User currentUser = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Set<Integer> memberUniversityIds = currentUser.getMemberships().stream()
                .map(membership -> membership.getUniversity().getUniversityId())
                .collect(Collectors.toSet());

        return universityRepository.findAll().stream()
                .map(university -> UniversityMapper.toUniversityDto(
                        university,
                        memberUniversityIds.contains(university.getUniversityId())
                ))
                .collect(Collectors.toList());
    }

    // --- getModulesByUniversity method has been moved to ModuleService ---

    @Transactional
    @CacheEvict(value = "universities", allEntries = true)
    public void joinUniversity(Integer universityId, UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));
        
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("University not found with id: " + universityId));

        UniversityMembership.UniversityMembershipId membershipId = new UniversityMembership.UniversityMembershipId();
        membershipId.setUserId(user.getUserId());
        membershipId.setUniversityId(university.getUniversityId());

        if (membershipRepository.existsById(membershipId)) {
            throw new IllegalArgumentException("User is already a member of this university.");
        }

        UniversityMembership membership = new UniversityMembership();
        membership.setId(membershipId);
        membership.setUser(user);
        membership.setUniversity(university);
        membership.setRole("student");
        membershipRepository.save(membership);       
        eventPublisher.publishEvent(new UserJoinedUniversityEvent(this, user, university)); // Add this line

    }

    @Transactional
    @CacheEvict(value = "universities", allEntries = true)
    public void unjoinUniversity(Integer universityId, UserDetailsImpl currentUser) {
        membershipRepository.deleteByUserIdAndUniversityId(currentUser.getId(), universityId);
    }
}