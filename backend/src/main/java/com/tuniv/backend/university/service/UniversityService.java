package com.tuniv.backend.university.service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.dto.UniversityBasicDto;
import com.tuniv.backend.university.dto.UniversityDto;
import com.tuniv.backend.university.mapper.UniversityMapper;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.university.model.UniversityRole;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.university.repository.UniversityMembershipRepository;
import com.tuniv.backend.university.repository.UniversityRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UniversityService {

    private final UniversityRepository universityRepository;
    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final UniversityMembershipRepository membershipRepository;
    private final UniversityMapper universityMapper;

    @Transactional(readOnly = true)
    public Page<UniversityBasicDto> getAllUniversities(Pageable pageable, UserDetailsImpl currentUser) {
        Page<University> universityPage = universityRepository.findAll(pageable);
        Set<Integer> memberUniversityIds = getMemberUniversityIds(currentUser);

        return universityPage.map(uni -> universityMapper.toUniversityBasicDto(uni, memberUniversityIds.contains(uni.getUniversityId())));
    }

    @Transactional(readOnly = true)
    public UniversityDto getUniversity(Integer universityId, UserDetailsImpl currentUser) {
        University university = universityRepository.findById(universityId)
            .orElseThrow(() -> new ResourceNotFoundException("University not found with id: " + universityId));

        Set<Integer> memberUniversityIds = getMemberUniversityIds(currentUser);
        boolean isMember = memberUniversityIds.contains(universityId);

        // Fetch modules and map them to DTOs separately
        List<ModuleDto> moduleDtos = moduleRepository.findByUniversityUniversityId(universityId).stream()
            .map(module -> universityMapper.toModuleDto(module, isMember))
            .collect(Collectors.toList());

        return universityMapper.toUniversityDto(university, isMember, moduleDtos);
    }

    @Transactional
    public void joinUniversity(Integer universityId, UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        University university = universityRepository.findById(universityId)
            .orElseThrow(() -> new ResourceNotFoundException("University not found"));

        boolean alreadyExists = membershipRepository.existsByUser_UserIdAndUniversity_UniversityId(user.getUserId(), university.getUniversityId());
        if (alreadyExists) {
            throw new IllegalArgumentException("User is already a member of this university.");
        }

        // ✅ CLEANER: Use the entity constructor
        UniversityMembership membership = new UniversityMembership(user, university, UniversityRole.UNVERIFIED_STUDENT);
        membershipRepository.save(membership);

        // ✅ UPDATE COUNT: This logic is now safe.
        // For production, this could be handled by a trigger or async event for even better performance.
        universityRepository.incrementMemberCount(universityId);
        log.info("User '{}' joined university '{}'. Member count updated.", user.getUsername(), university.getName());
    }

    @Transactional
    public void unjoinUniversity(Integer universityId, UserDetailsImpl currentUser) {
        UniversityMembership membership = membershipRepository
            .findByUser_UserIdAndUniversity_UniversityId(currentUser.getId(), universityId)
            .orElseThrow(() -> new ResourceNotFoundException("Membership not found for this user and university."));

        membershipRepository.delete(membership);
        
        // ✅ UPDATE COUNT:
        universityRepository.decrementMemberCount(universityId);
        log.info("User with id {} left university with id {}. Member count updated.", currentUser.getId(), universityId);
    }

    @Transactional(readOnly = true)
    public List<UniversityBasicDto> getJoinedUniversities(UserDetailsImpl currentUser) {
        if (currentUser == null) return Collections.emptyList();
        
        // ✅ EFFICIENT: Fetch universities directly via a JOIN, instead of going through the User entity.
        return membershipRepository.findUniversitiesByUserId(currentUser.getId()).stream()
            .map(university -> universityMapper.toUniversityBasicDto(university, true))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UniversityBasicDto> getTopUniversities(UserDetailsImpl currentUser) {
        // ✅ CORRECT: Sort by the denormalized `memberCount` field.
        List<University> topUniversities = universityRepository.findTop5ByOrderByMemberCountDesc();
        Set<Integer> memberUniversityIds = getMemberUniversityIds(currentUser);
        
        return topUniversities.stream()
            .map(uni -> universityMapper.toUniversityBasicDto(uni, memberUniversityIds.contains(uni.getUniversityId())))
            .collect(Collectors.toList());
    }

    /**
     * ✅ REFACTORED: Private helper to safely get the university IDs a user is a member of.
     * This replaces all broken `currentUser.getMemberships()` calls.
     */
    private Set<Integer> getMemberUniversityIds(UserDetailsImpl currentUser) {
        if (currentUser == null) {
            return Collections.emptySet();
        }
        return membershipRepository.findUniversityIdsByUserId(currentUser.getId());
    }
}