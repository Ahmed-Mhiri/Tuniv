package com.tuniv.backend.university.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.dto.UniversityDto; // <-- IMPORT ADDED
import com.tuniv.backend.university.mapper.UniversityMapper;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.university.repository.UniversityMembershipRepository;
import com.tuniv.backend.university.repository.UniversityRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UniversityService {

    private final UniversityRepository universityRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final UniversityMembershipRepository membershipRepository;
    
    // --- THIS IS THE FIX ---
    @Transactional(readOnly = true) // Keep the database session open for this method
    @Cacheable("universities")
    public List<UniversityDto> getAllUniversities(UserDetailsImpl currentUserDetails) {
        // First, fetch the full user object to access its relationships
        User currentUser = userRepository.findById(currentUserDetails.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get the set of IDs for universities the user is a member of, directly from the user object
        Set<Integer> memberUniversityIds = currentUser.getMemberships().stream()
                .map(membership -> membership.getUniversity().getUniversityId())
                .collect(Collectors.toSet());

        // Now, map all universities and apply the membership status
        return universityRepository.findAll().stream()
                .map(university -> UniversityMapper.toUniversityDto(
                    university,
                    memberUniversityIds.contains(university.getUniversityId())
                ))
                .collect(Collectors.toList());
    }

    public List<ModuleDto> getModulesByUniversity(Integer universityId) {
        if (!universityRepository.existsById(universityId)) {
            throw new ResourceNotFoundException("University not found with id: " + universityId);
        }
        return moduleRepository.findByUniversityUniversityId(universityId).stream()
                .map(UniversityMapper::toModuleDto) // Use the central mapper
                .collect(Collectors.toList());
    }

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
    }

    @Transactional
    @CacheEvict(value = "universities", allEntries = true)
    public void unjoinUniversity(Integer universityId, UserDetailsImpl currentUser) {
        // --- FIX: Use the more robust custom delete query ---
        membershipRepository.deleteByUserIdAndUniversityId(currentUser.getId(), universityId);
    }
    
    
    
}