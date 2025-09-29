package com.tuniv.backend.community.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.community.dto.CommunityCreateRequest;
import com.tuniv.backend.community.dto.CommunityDetailDto;
import com.tuniv.backend.community.dto.CommunitySummaryDto;
import com.tuniv.backend.community.mapper.CommunityMapper;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.community.model.CommunityMembership;
import com.tuniv.backend.community.model.CommunityRole;
import com.tuniv.backend.community.repository.CommunityMembershipRepository;
import com.tuniv.backend.community.repository.CommunityRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.repository.UniversityRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;

import com.tuniv.backend.community.dto.CommunityStatsDto;
import com.tuniv.backend.community.dto.CommunityTopicStatsDto;
import com.tuniv.backend.community.dto.CommunityUpdateRequest;
import com.tuniv.backend.community.dto.CommunityWithStatsDto;
import com.tuniv.backend.community.dto.TrendingCommunityDto;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.model.TopicType;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.service.TopicService;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final CommunityMapper communityMapper;
    private final TopicService topicService;
    private final TopicRepository topicRepository;

    @Transactional
    public CommunityDetailDto createCommunity(CommunityCreateRequest request, UserDetailsImpl currentUserDetails) {
        User creator = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Community community = new Community();
        community.setName(request.name());
        community.setDescription(request.description());
        community.setCreator(creator);

        if (request.universityId() != null) {
            University university = universityRepository.findById(request.universityId())
                    .orElseThrow(() -> new ResourceNotFoundException("University not found"));
            community.setUniversity(university);
        }

        CommunityMembership initialMembership = new CommunityMembership();
        initialMembership.setUser(creator);
        initialMembership.setCommunity(community);
        initialMembership.setRole(CommunityRole.MODERATOR);
        
        community.getMembers().add(initialMembership);
        community.incrementMemberCount();

        Community savedCommunity = communityRepository.save(community);
        return communityMapper.toDetailDto(savedCommunity, creator);
    }

    @Transactional(readOnly = true)
    public CommunityDetailDto getCommunityDetails(Integer communityId, UserDetailsImpl currentUserDetails) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        User currentUser = (currentUserDetails != null) ? 
            userRepository.findById(currentUserDetails.getId()).orElse(null) : null;
        
        return communityMapper.toDetailDto(community, currentUser);
    }

    @Transactional
    public void joinCommunity(Integer communityId, UserDetailsImpl currentUserDetails) {
        User user = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        boolean isAlreadyMember = community.getMembers().stream()
                .anyMatch(m -> m.getUser().equals(user));

        if (isAlreadyMember) {
            throw new IllegalArgumentException("User is already a member of this community.");
        }

        CommunityMembership membership = new CommunityMembership();
        membership.setUser(user);
        membership.setCommunity(community);
        membership.setRole(CommunityRole.MEMBER);
        membershipRepository.save(membership);

        community.incrementMemberCount();
        communityRepository.save(community);
    }

    @Transactional
    public void leaveCommunity(Integer communityId, UserDetailsImpl currentUserDetails) {
        CommunityMembership.CommunityMembershipId membershipId = 
            new CommunityMembership.CommunityMembershipId(currentUserDetails.getId(), communityId);
            
        membershipRepository.findById(membershipId).ifPresent(membership -> {
            membershipRepository.delete(membership);
            
            Community community = membership.getCommunity();
            community.decrementMemberCount();
            communityRepository.save(community);
        });
    }

    @Transactional(readOnly = true)
    public Page<CommunitySummaryDto> getAllCommunities(String search, Integer universityId, Pageable pageable, UserDetailsImpl currentUser) {
        Page<Community> communities = communityRepository.findAllWithFilters(search, universityId, pageable);
        return communities.map(communityMapper::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryDto> getTopCommunities(UserDetailsImpl currentUser) {
        List<Community> communities = communityRepository.findTopCommunities(PageRequest.of(0, 10));
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
    public Page<TopicSummaryDto> getTopicsByCommunity(Integer communityId, Pageable pageable, UserDetailsImpl currentUser) {
        return topicService.getTopicsByCommunity(communityId, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getTopicsByCommunityAndType(Integer communityId, TopicType topicType, Pageable pageable, UserDetailsImpl currentUser) {
        Page<TopicSummaryDto> allTopics = topicService.getTopicsByCommunity(communityId, pageable, currentUser);
        
        List<TopicSummaryDto> filteredContent = allTopics.getContent().stream()
                .filter(topic -> topic.topicType() == topicType)
                .collect(Collectors.toList());
        
        return new PageImpl<>(filteredContent, pageable, allTopics.getTotalElements());
    }

    // ✅ NEW: Enhanced community statistics with topic data
    @Transactional(readOnly = true)
    public CommunityStatsDto getCommunityStats(Integer communityId) {
        Community community = communityRepository.findWithTopicsById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        CommunityTopicStatsDto statsDto = communityRepository.getCommunityTopicStats(communityId)
                .orElse(new CommunityTopicStatsDto(
                    communityId,
                    community.getName(),
                    0L, 0L, 0L, 0L
                ));

        return communityMapper.toStatsDto(statsDto, community);
    }

    // ✅ NEW: Get community details with statistics
    @Transactional(readOnly = true)
    public CommunityWithStatsDto getCommunityWithStats(Integer communityId, UserDetailsImpl currentUser) {
        Community community = communityRepository.findWithTopicsById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        CommunityTopicStatsDto statsDto = communityRepository.getCommunityTopicStats(communityId)
                .orElse(new CommunityTopicStatsDto(
                    communityId,
                    community.getName(),
                    0L, 0L, 0L, 0L
                ));

        User currentUserEntity = (currentUser != null) ? 
            userRepository.findById(currentUser.getId()).orElse(null) : null;

        return communityMapper.toCommunityWithStatsDto(community, statsDto, currentUserEntity);
    }

    // ✅ NEW: Get trending communities with activity metrics
    @Transactional(readOnly = true)
    public List<TrendingCommunityDto> getTrendingCommunities(UserDetailsImpl currentUser) {
        Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        List<Community> trendingCommunities = communityRepository.findTrendingCommunities(
            oneWeekAgo, 5L, PageRequest.of(0, 10)
        );

        return trendingCommunities.stream()
                .map(community -> {
                    // Calculate recent activity count for this community
                    Long recentActivityCount = topicRepository.countByCommunityAndCreatedAfter(
                        community, oneWeekAgo);
                    return communityMapper.toTrendingCommunityDto(community, recentActivityCount);
                })
                .collect(Collectors.toList());
    }

    // ✅ NEW: Advanced community search with multiple filters
    @Transactional(readOnly = true)
    public Page<CommunitySummaryDto> searchCommunitiesWithFilters(
            String search, 
            Integer minTopics, 
            Integer minMembers, 
            Pageable pageable, 
            UserDetailsImpl currentUser) {
        
        Page<Community> communities = communityRepository.findAllWithAdvancedFilters(
            search, minTopics, minMembers, pageable
        );
        return communities.map(communityMapper::toSummaryDto);
    }

    // ✅ NEW: Get popular communities with minimum member threshold
    @Transactional(readOnly = true)
    public List<CommunitySummaryDto> getPopularCommunities(Integer minMembers, UserDetailsImpl currentUser) {
        List<Community> popularCommunities = communityRepository.findPopularCommunities(
            minMembers != null ? minMembers : 10, 
            PageRequest.of(0, 20)
        );
        return communityMapper.toSummaryDtoList(popularCommunities);
    }

    // ✅ NEW: Get active communities (recent topic activity)
    @Transactional(readOnly = true)
    public List<CommunitySummaryDto> getActiveCommunities(UserDetailsImpl currentUser) {
        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        List<Community> activeCommunities = communityRepository.findActiveCommunitiesSince(
            oneDayAgo, PageRequest.of(0, 15)
        );
        return communityMapper.toSummaryDtoList(activeCommunities);
    }

    // ✅ NEW: Get communities by university with topic count ordering
    @Transactional(readOnly = true)
    public List<CommunitySummaryDto> getCommunitiesByUniversity(Integer universityId, UserDetailsImpl currentUser) {
        List<Community> communities = communityRepository.findByUniversityIdOrderByTopicCountDesc(universityId);
        return communityMapper.toSummaryDtoList(communities);
    }

    // ✅ NEW: Update community information
    @Transactional
    public CommunityDetailDto updateCommunity(Integer communityId, CommunityUpdateRequest request, UserDetailsImpl currentUser) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        boolean isModerator = community.getMembers().stream()
                .anyMatch(m -> m.getUser().getUserId().equals(currentUser.getId()) && 
                              m.getRole() == CommunityRole.MODERATOR);
        
        if (!isModerator) {
            throw new AccessDeniedException("Only moderators can update community information");
        }

        if (request.name() != null) {
            community.setName(request.name());
        }
        if (request.description() != null) {
            community.setDescription(request.description());
        }

        Community updatedCommunity = communityRepository.save(community);
        User currentUserEntity = userRepository.findById(currentUser.getId()).orElse(null);
        
        return communityMapper.toDetailDto(updatedCommunity, currentUserEntity);
    }

    // ✅ NEW: Check if user can access community
    @Transactional(readOnly = true)
    public boolean canAccessCommunity(Integer communityId, UserDetailsImpl currentUser) {
        return communityRepository.existsAndAccessible(communityId);
    }

    // ✅ NEW: Get community topic type distribution
    @Transactional(readOnly = true)
    public Map<TopicType, Long> getCommunityTopicDistribution(Integer communityId) {
        CommunityTopicStatsDto statsDto = communityRepository.getCommunityTopicStats(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        Map<TopicType, Long> distribution = new HashMap<>();
        distribution.put(TopicType.QUESTION, statsDto.questionTopics());
        distribution.put(TopicType.POST, statsDto.postTopics());
        
        return distribution;
    }
}