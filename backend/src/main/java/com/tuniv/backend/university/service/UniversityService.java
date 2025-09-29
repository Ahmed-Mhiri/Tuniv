package com.tuniv.backend.university.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.UserJoinedUniversityEvent;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.dto.UniversityDto;
import com.tuniv.backend.university.mapper.UniversityMapper;
import com.tuniv.backend.university.model.Module; // <-- IMPORT ADDED
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.university.model.UniversitySpecification;
import com.tuniv.backend.university.model.UserRoleEnum;
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
    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final UniversityMembershipRepository membershipRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TopicRepository topicRepository; // ✅ ADDED: For topic operations



    @Transactional(readOnly = true)
    public Page<UniversityDto> getAllUniversities(String searchTerm, Pageable pageable, UserDetailsImpl currentUserDetails) {
        // 1. Create the search specification
        Specification<University> spec = UniversitySpecification.searchByName(searchTerm);

        // 2. Fetch the paginated list of universities from the repository
        Page<University> universityPage = universityRepository.findAll(spec, pageable);

        // 3. Determine which universities the current user is a member of
        final Set<Integer> memberUniversityIds;
        if (currentUserDetails != null) {
            User currentUser = userRepository.findById(currentUserDetails.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            memberUniversityIds = currentUser.getMemberships().stream()
                    .map(membership -> membership.getUniversity().getUniversityId())
                    .collect(Collectors.toSet());
        } else {
            memberUniversityIds = Collections.emptySet(); // Anonymous user is a member of nothing
        }
        
        // 4. Map the Page<University> to a Page<UniversityDto>
        return universityPage.map(university -> UniversityMapper.toUniversityDto(
                university,
                memberUniversityIds.contains(university.getUniversityId())
        ));
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
        membership.setRole(UserRoleEnum.STUDENT);
        membershipRepository.save(membership);

        // ✅ Update university member count
        university.incrementMemberCount();
        universityRepository.save(university);
        
        eventPublisher.publishEvent(new UserJoinedUniversityEvent(this, user, university));
    }

    @Transactional
    @CacheEvict(value = "universities", allEntries = true)
    public void unjoinUniversity(Integer universityId, UserDetailsImpl currentUser) {
        University university = universityRepository.findById(universityId)
            .orElseThrow(() -> new ResourceNotFoundException("University not found with id: " + universityId));
        
        membershipRepository.deleteByUserIdAndUniversityId(currentUser.getId(), universityId);
        
        // ✅ Update university member count
        university.decrementMemberCount();
        universityRepository.save(university);
    }

    @Transactional(readOnly = true)
    public List<UniversityDto> getJoinedUniversities(UserDetailsImpl currentUserDetails) {
    if (currentUserDetails == null) {
        return Collections.emptyList(); // A non-logged-in user has joined nothing
    }

    User currentUser = userRepository.findById(currentUserDetails.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    return currentUser.getMemberships().stream()
            .map(membership -> {
                // Map the University from the membership to its DTO
                // The 'isMember' flag is always true here by definition
                return UniversityMapper.toUniversityDto(membership.getUniversity(), true);
            })
            .collect(Collectors.toList());
}

@Transactional(readOnly = true)
    public List<UniversityDto> getTopUniversities(UserDetailsImpl currentUserDetails) {
        List<University> topUniversities = universityRepository.findTop5ByOrderByMembershipsSizeDesc();

        final Set<Integer> memberUniversityIds;
        if (currentUserDetails != null) {
            // This logic is duplicated, consider refactoring to a private helper method if used often
            User currentUser = userRepository.findById(currentUserDetails.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            memberUniversityIds = currentUser.getMemberships().stream()
                    .map(membership -> membership.getUniversity().getUniversityId())
                    .collect(Collectors.toSet());
        } else {
            memberUniversityIds = Collections.emptySet();
        }

        return topUniversities.stream()
                .map(university -> UniversityMapper.toUniversityDto(
                        university,
                        memberUniversityIds.contains(university.getUniversityId())
                ))
                .collect(Collectors.toList());
    }

    // ✅ NEW: Get topics by university
    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getTopicsByUniversity(Integer universityId, Pageable pageable, UserDetailsImpl currentUser) {
        if (!universityRepository.existsById(universityId)) {
            throw new ResourceNotFoundException("University not found with id: " + universityId);
        }
        
        Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        
        // Get all module IDs for this university
        List<Integer> moduleIds = moduleRepository.findByUniversityUniversityId(universityId)
                .stream()
                .map(Module::getModuleId)
                .collect(Collectors.toList());
        
        if (moduleIds.isEmpty()) {
            return Page.empty(pageable);
        }
        
        return topicRepository.findTopicSummariesByModuleIdIn(moduleIds, currentUserId, pageable);
    }

    // ✅ NEW: Update university topic count when a topic is created/deleted in its modules
    @Transactional
    public void updateUniversityTopicCount(Integer universityId) {
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("University not found"));
        
        // Calculate total topics across all modules
        int totalTopics = moduleRepository.findByUniversityUniversityId(universityId)
                .stream()
                .mapToInt(Module::getTopicCount)
                .sum();
        
        university.setTopicCount(totalTopics);
        universityRepository.save(university);
    }
}