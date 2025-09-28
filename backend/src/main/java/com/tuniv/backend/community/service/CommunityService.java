package com.tuniv.backend.community.service;

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
import com.tuniv.backend.qa.dto.QuestionSummaryDto;
import com.tuniv.backend.qa.service.QuestionService;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.repository.UniversityRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final CommunityMapper communityMapper;
    private final QuestionService questionService;

    @Transactional
    public CommunityDetailDto createCommunity(CommunityCreateRequest request, UserDetailsImpl currentUserDetails) {
        User creator = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Community community = new Community();
        community.setName(request.name());
        community.setDescription(request.description());
        community.setCreator(creator);

        // If a universityId is provided, link it to the community
        if (request.universityId() != null) {
            University university = universityRepository.findById(request.universityId())
                    .orElseThrow(() -> new ResourceNotFoundException("University not found"));
            community.setUniversity(university);
        }

        // The creator automatically becomes the first member and a moderator.
        CommunityMembership initialMembership = new CommunityMembership();
        initialMembership.setUser(creator);
        initialMembership.setCommunity(community);
        initialMembership.setRole(CommunityRole.MODERATOR);
        
        // ✅ Use helper method instead of direct field assignment
        community.getMembers().add(initialMembership);
        community.incrementMemberCount(); // ✅ Use helper method

        Community savedCommunity = communityRepository.save(community);
        return communityMapper.toDetailDto(savedCommunity, creator);
    }

    @Transactional(readOnly = true)
    public CommunityDetailDto getCommunityDetails(Integer communityId, UserDetailsImpl currentUserDetails) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        User currentUser = (currentUserDetails != null) ? userRepository.findById(currentUserDetails.getId()).orElse(null) : null;
        
        // The mapper handles the logic to determine if the user is a member/moderator
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
        membership.setRole(CommunityRole.MEMBER); // Default role
        membershipRepository.save(membership);

        // ✅ Update the denormalized member count using helper method
        community.incrementMemberCount();
        communityRepository.save(community);
    }

    @Transactional
    public void leaveCommunity(Integer communityId, UserDetailsImpl currentUserDetails) {
        // Find the specific membership record to delete
        CommunityMembership.CommunityMembershipId membershipId = 
            new CommunityMembership.CommunityMembershipId(currentUserDetails.getId(), communityId);
            
        membershipRepository.findById(membershipId).ifPresent(membership -> {
            membershipRepository.delete(membership);
            
            // ✅ Decrement the member count using helper method
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
        // Get top 10 communities by member count
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
    public Page<QuestionSummaryDto> getQuestionsByCommunity(Integer communityId, Pageable pageable, UserDetailsImpl currentUser) {
        // Delegate to QuestionService which already has this functionality
        return questionService.getQuestionsByCommunity(communityId, pageable, currentUser);
    }
}