package com.tuniv.backend.community.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.authorization.model.CommunityPermissions;
import com.tuniv.backend.authorization.service.PermissionService;
import com.tuniv.backend.community.dto.CommunityCreateRequest;
import com.tuniv.backend.community.dto.CommunityDetailDto;
import com.tuniv.backend.community.dto.CommunityStatsDto;
import com.tuniv.backend.community.dto.CommunitySummaryDto;
import com.tuniv.backend.community.dto.CommunityTopicStatsDto;
import com.tuniv.backend.community.dto.CommunityWithStatsDto;
import com.tuniv.backend.community.dto.TrendingCommunityDto;
import com.tuniv.backend.community.dto.CommunityUpdateRequest;
import com.tuniv.backend.community.dto.ModerationStatusDto;
import com.tuniv.backend.community.mapper.CommunityMapper;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.community.model.CommunityJoinPolicy;
import com.tuniv.backend.community.model.CommunityMembership;
import com.tuniv.backend.community.model.CommunityRole;
import com.tuniv.backend.community.model.DefaultCommunityRoles;
import com.tuniv.backend.community.repository.CommunityMembershipRepository;
import com.tuniv.backend.community.repository.CommunityRepository;
import com.tuniv.backend.community.repository.CommunityRoleRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.model.TopicType;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.service.TopicService;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.repository.UniversityRepository;
import com.tuniv.backend.university.repository.UniversityMembershipRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final UniversityMembershipRepository universityMembershipRepository;
    private final CommunityMapper communityMapper;
    private final TopicService topicService;
    private final TopicRepository topicRepository;
    private final CommunityRoleRepository roleRepository;
    private final PermissionService permissionService;

    @Transactional
    public CommunityDetailDto createCommunity(CommunityCreateRequest request, UserDetailsImpl currentUserDetails) {
        User creator = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if community name already exists
        if (communityRepository.findByNameIgnoreCase(request.name()).isPresent()) {
            throw new IllegalArgumentException("Community with this name already exists");
        }

        Community community = new Community();
        community.setName(request.name());
        community.setDescription(request.description());
        community.setCreator(creator);

        if (request.universityId() != null) {
            University university = universityRepository.findById(request.universityId())
                    .orElseThrow(() -> new ResourceNotFoundException("University not found"));
            community.setUniversity(university);
        }

        // Save community first to get an ID
        Community savedCommunity = communityRepository.save(community);

        // Fetch the 'MODERATOR' role from the repository
        CommunityRole moderatorRole = roleRepository.findByNameAndIsSystemRole(DefaultCommunityRoles.COMMUNITY_MODERATOR.getName(), true)
                .orElseThrow(() -> new RuntimeException("Default moderator role not found. Please seed roles."));

        CommunityMembership initialMembership = new CommunityMembership();
        initialMembership.setUser(creator);
        initialMembership.setCommunity(savedCommunity);
        initialMembership.setRole(moderatorRole);
        
        // Save the membership to its own repository
        membershipRepository.save(initialMembership);
        
        // Use the repository to increment the counter
        communityRepository.incrementMemberCount(savedCommunity.getCommunityId());

        // ✅ FIXED: Use the correct mapper method with membership and null moderation status
        return communityMapper.toDetailDto(savedCommunity, initialMembership, null);
    }

    @Transactional(readOnly = true)
    public CommunityDetailDto getCommunityDetails(Integer communityId, UserDetailsImpl currentUserDetails) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        // ✅ FIXED: Add logic to fetch membership and moderation status
        CommunityMembership membership = null;
        ModerationStatusDto modStatus = null; // TODO: Inject ModerationService to fetch this

        if (currentUserDetails != null) {
            membership = membershipRepository
                .findById_UserIdAndId_CommunityId(currentUserDetails.getId(), communityId)
                .orElse(null);
            // modStatus = moderationService.getStatus(...);
        }
        
        return communityMapper.toDetailDto(community, membership, modStatus);
    }

    @Transactional
    public void joinCommunity(Integer communityId, UserDetailsImpl currentUserDetails) {
        User user = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        // Check for existing membership
        if (membershipRepository.existsById_UserIdAndId_CommunityId(user.getUserId(), communityId)) {
            throw new IllegalArgumentException("User is already a member of this community.");
        }

        // ** NEW JOIN POLICY LOGIC **
        CommunityJoinPolicy policy = community.getJoinPolicy();

        switch (policy) {
            case OPEN:
                // This is the original logic
                createNewMembership(user, community);
                break;

            case APPROVAL:
                // TODO: This requires implementing the JoinRequest system from Critique II.1
                // For now, create a JoinRequest (assuming you have a JoinRequest entity and repository)
                // JoinRequest request = new JoinRequest(user, community, "User requested to join");
                // joinRequestRepository.save(request);
                // DO NOT create a membership or increment the counter
                throw new UnsupportedOperationException("Join requests are not yet implemented. Cannot join an 'APPROVAL' community.");

            case UNIVERSITY_ONLY:
                // Check if user is a member of the community's university
                if (community.getUniversity() == null || 
                    !universityMembershipRepository.existsByUserAndUniversity(user, community.getUniversity())) {
                    throw new AccessDeniedException("Only members of " + community.getUniversity().getName() + " can join this community.");
                }
                createNewMembership(user, community);
                break;

            case RESTRICTED:
                throw new AccessDeniedException("This community is invite-only.");
        }
    }

    // ** HELPER METHOD FOR CREATING MEMBERSHIPS **
    private void createNewMembership(User user, Community community) {
        CommunityMembership membership = new CommunityMembership();
        membership.setUser(user);
        membership.setCommunity(community);

        CommunityRole memberRole = roleRepository.findByNameAndIsSystemRole(DefaultCommunityRoles.COMMUNITY_MEMBER.getName(), true)
                .orElseThrow(() -> new RuntimeException("Default member role not found. Please seed roles."));
        membership.setRole(memberRole);

        membershipRepository.save(membership);
        communityRepository.incrementMemberCount(community.getCommunityId());
    }

    @Transactional
    public void leaveCommunity(Integer communityId, UserDetailsImpl currentUserDetails) {
        // FIXED: Use the correct repository method
        Optional<CommunityMembership> membership = membershipRepository.findById_UserIdAndId_CommunityId(
            currentUserDetails.getId(), communityId);
        
        if (membership.isPresent()) {
            membershipRepository.delete(membership.get());
            
            Community community = membership.get().getCommunity();
            // Use the repository to decrement the counter
            communityRepository.decrementMemberCount(community.getCommunityId());
        }
    }

    @Transactional(readOnly = true)
    public Page<CommunitySummaryDto> getAllCommunities(String search, Integer universityId, Pageable pageable, UserDetailsImpl currentUser) {
        Page<Community> communities;
        
        if (search != null && !search.trim().isEmpty() && universityId != null) {
            // This method doesn't exist in your repository, so we'll filter manually
            communities = communityRepository.findByNameContainingIgnoreCase(search, pageable);
            // Filter by university in memory (not ideal but works for now)
            List<Community> filtered = communities.getContent().stream()
                .filter(c -> c.getUniversity() != null && c.getUniversity().getUniversityId().equals(universityId))
                .collect(Collectors.toList());
            return new PageImpl<>(communityMapper.toSummaryDtoList(filtered), pageable, filtered.size());
        } else if (search != null && !search.trim().isEmpty()) {
            communities = communityRepository.findByNameContainingIgnoreCase(search, pageable);
        } else if (universityId != null) {
            // This method doesn't exist in your repository, so we'll use findAll and filter
            communities = communityRepository.findAll(pageable);
            List<Community> filtered = communities.getContent().stream()
                .filter(c -> c.getUniversity() != null && c.getUniversity().getUniversityId().equals(universityId))
                .collect(Collectors.toList());
            return new PageImpl<>(communityMapper.toSummaryDtoList(filtered), pageable, filtered.size());
        } else {
            communities = communityRepository.findAll(pageable);
        }
        
        return communities.map(communityMapper::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryDto> getTopCommunities(UserDetailsImpl currentUser) {
        List<Community> communities = communityRepository.findByOrderByMemberCountDesc(PageRequest.of(0, 10));
        return communityMapper.toSummaryDtoList(communities);
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryDto> getJoinedCommunities(UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<Community> communities = communityRepository.findCommunitiesByUserId(user.getUserId());
        return communityMapper.toSummaryDtoList(communities);
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryDto> getGlobalCommunities(UserDetailsImpl currentUser) {
        List<Community> communities = communityRepository.findByUniversityIsNull();
        return communityMapper.toSummaryDtoList(communities);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getTopicsByCommunity(
            Integer communityId, TopicType topicType, Pageable pageable, UserDetailsImpl currentUser) {
        // Modified to accept TopicType parameter and pass it to TopicService
        return topicService.getTopicsByCommunity(communityId, topicType, pageable, currentUser);
    }

    // REMOVED: getTopicsByCommunityAndType method (line 227) - it's no longer needed

    @Transactional(readOnly = true)
    public CommunityStatsDto getCommunityStats(Integer communityId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        // Use repository count methods for stats
        long totalTopics = communityRepository.countTopicsByCommunityId(communityId);
        long solvedTopics = communityRepository.countSolvedTopicsByCommunityId(communityId);
        long questionTopics = communityRepository.countTopicsByCommunityIdAndType(communityId, TopicType.QUESTION);
        long postTopics = communityRepository.countTopicsByCommunityIdAndType(communityId, TopicType.DISCUSSION);

        // Manually build the DTO
        CommunityTopicStatsDto statsDto = new CommunityTopicStatsDto(
                communityId,
                community.getName(),
                totalTopics,
                solvedTopics,
                questionTopics,
                postTopics
        );

        return communityMapper.toStatsDto(statsDto, community);
    }

    @Transactional(readOnly = true)
    public CommunityWithStatsDto getCommunityWithStats(Integer communityId, UserDetailsImpl currentUser) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        // ✅ FIXED: Add logic to fetch membership and moderation status
        CommunityMembership membership = null;
        ModerationStatusDto modStatus = null; // TODO: Inject ModerationService to fetch this

        if (currentUser != null) {
            User currentUserEntity = userRepository.findById(currentUser.getId()).orElse(null); // You already have this
            membership = membershipRepository
                .findById_UserIdAndId_CommunityId(currentUser.getId(), communityId)
                .orElse(null);
            // modStatus = moderationService.getStatus(...);
        }

        CommunityStatsDto stats = getCommunityStats(communityId);
        CommunityDetailDto detailDto = communityMapper.toDetailDto(community, membership, modStatus);
        
        return new CommunityWithStatsDto(detailDto, stats);
    }

    @Transactional(readOnly = true)
    public List<TrendingCommunityDto> getTrendingCommunities(UserDetailsImpl currentUser) {
        Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        
        // Get trending communities using repository method
        List<Community> trendingCommunities = communityRepository.findTrendingCommunities(
            oneWeekAgo, 5L, PageRequest.of(0, 10)
        );

        return trendingCommunities.stream()
                .map(community -> {
                    // Calculate recent activity count for this community
                    Long recentActivityCount = communityRepository.countTopicsByCommunityIdAndType(
                        community.getCommunityId(), TopicType.QUESTION) + 
                        communityRepository.countTopicsByCommunityIdAndType(community.getCommunityId(), TopicType.DISCUSSION);
                    return communityMapper.toTrendingCommunityDto(community, recentActivityCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CommunitySummaryDto> searchCommunitiesWithFilters(
            String search, 
            Integer minTopics, 
            Integer minMembers, 
            Pageable pageable, 
            UserDetailsImpl currentUser) {

        Specification<Community> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
            if (minTopics != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("topicCount"), minTopics));
            }
            if (minMembers != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("memberCount"), minMembers));
            }

            // Add default visibility/active filters if needed
            // predicates.add(cb.isTrue(root.get("isActive")));
            // predicates.add(cb.equal(root.get("visibility"), CommunityVisibility.PUBLIC));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Community> communities = communityRepository.findAll(spec, pageable);
        return communities.map(communityMapper::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryDto> getPopularCommunities(Integer minMembers, UserDetailsImpl currentUser) {
        List<Community> popularCommunities = communityRepository.findByMemberCountGreaterThanEqualOrderByMemberCountDesc(
            minMembers != null ? minMembers : 10, 
            PageRequest.of(0, 20)
        );
        return communityMapper.toSummaryDtoList(popularCommunities);
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryDto> getActiveCommunities(UserDetailsImpl currentUser) {
        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        
        // Get communities with recent topic activity
        List<Community> activeCommunities = communityRepository.findActiveCommunitiesSince(oneDayAgo, PageRequest.of(0, 15));
        return communityMapper.toSummaryDtoList(activeCommunities);
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryDto> getCommunitiesByUniversity(Integer universityId, UserDetailsImpl currentUser) {
        List<Community> communities = communityRepository.findByUniversity_UniversityIdOrderByTopicCountDesc(universityId);
        return communityMapper.toSummaryDtoList(communities);
    }

    @Transactional
    public CommunityDetailDto updateCommunity(Integer communityId, CommunityUpdateRequest request, UserDetailsImpl currentUser) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        // Use PermissionService to check authorization
        if (!permissionService.hasPermission(
                currentUser.getId(), 
                CommunityPermissions.COMMUNITY_SETTINGS_EDIT.getName(), 
                community)) {
            throw new AccessDeniedException("Insufficient permissions to update community settings");
        }

        if (request.name() != null && !request.name().trim().isEmpty()) {
            // Check if new name is already taken by another community
            Optional<Community> existingCommunity = communityRepository.findByNameIgnoreCase(request.name());
            if (existingCommunity.isPresent() && !existingCommunity.get().getCommunityId().equals(communityId)) {
                throw new IllegalArgumentException("Community name already exists");
            }
            community.setName(request.name());
        }
        if (request.description() != null) {
            community.setDescription(request.description());
        }

        Community updatedCommunity = communityRepository.save(community);
        
        // ✅ FIXED: Use the membership that we already have and null for moderation status
        Optional<CommunityMembership> membership = membershipRepository.findById_UserIdAndId_CommunityId(
            currentUser.getId(), communityId);
        
        return communityMapper.toDetailDto(updatedCommunity, membership.get(), null);
    }

    @Transactional(readOnly = true)
    public boolean canAccessCommunity(Integer communityId, UserDetailsImpl currentUser) {
        return communityRepository.existsById(communityId);
    }

    @Transactional(readOnly = true)
    public Map<TopicType, Long> getCommunityTopicDistribution(Integer communityId) {
        if (!communityRepository.existsById(communityId)) {
             throw new ResourceNotFoundException("Community not found");
        }
        
        // Fetch stats using repository methods
        long questionTopics = communityRepository.countTopicsByCommunityIdAndType(communityId, TopicType.QUESTION);
        long postTopics = communityRepository.countTopicsByCommunityIdAndType(communityId, TopicType.DISCUSSION);

        Map<TopicType, Long> distribution = new HashMap<>();
        distribution.put(TopicType.QUESTION, questionTopics);
        distribution.put(TopicType.DISCUSSION, postTopics);
        
        return distribution;
    }

    @Transactional(readOnly = true)
    public boolean isUserMember(Integer communityId, UserDetailsImpl currentUser) {
        if (currentUser == null) {
            return false;
        }
        return membershipRepository.existsById_UserIdAndId_CommunityId(currentUser.getId(), communityId);
    }

    @Transactional(readOnly = true)
    public boolean isUserModerator(Integer communityId, UserDetailsImpl currentUser) {
        if (currentUser == null) {
            return false;
        }
        
        Optional<CommunityMembership> membership = membershipRepository.findById_UserIdAndId_CommunityId(
            currentUser.getId(), communityId);
        
        if (membership.isEmpty()) {
            return false;
        }
        
        CommunityRole moderatorRole = roleRepository.findByNameAndIsSystemRole(
            DefaultCommunityRoles.COMMUNITY_MODERATOR.getName(), true)
                .orElse(null);
                
        return moderatorRole != null && membership.get().getRole().equals(moderatorRole);
    }

    @Transactional(readOnly = true)
    public CommunitySummaryDto getCommunitySummary(Integer communityId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        return communityMapper.toSummaryDto(community);
    }

    @Transactional
    public void deleteCommunity(Integer communityId, UserDetailsImpl currentUser) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        // Use PermissionService to check authorization
        if (!permissionService.hasPermission(
                currentUser.getId(), 
                CommunityPermissions.COMMUNITY_SETTINGS_EDIT.getName(), 
                community)) {
            throw new AccessDeniedException("Insufficient permissions to delete this community");
        }

        // Delete all memberships first - FIXED METHOD NAME
        List<CommunityMembership> memberships = membershipRepository.findById_CommunityId(communityId);
        membershipRepository.deleteAll(memberships);

        // Then delete the community
        communityRepository.delete(community);
    }
}