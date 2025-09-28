package com.tuniv.backend.community.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tuniv.backend.community.dto.CommunityDetailDto;
import com.tuniv.backend.community.dto.CommunitySummaryDto;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.community.model.CommunityMembership;
import com.tuniv.backend.community.model.CommunityRole;
import com.tuniv.backend.user.dto.UserSummaryDto;
import com.tuniv.backend.user.mapper.UserMapper;
import com.tuniv.backend.user.model.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommunityMapper {

    private final UserMapper userMapper;

    public CommunitySummaryDto toSummaryDto(Community community) {
        if (community == null) {
            return null;
        }
        
        String universityName = (community.getUniversity() != null) ? community.getUniversity().getName() : null;
        
        return new CommunitySummaryDto(
            community.getCommunityId(),
            community.getName(),
            community.getMemberCount(),
            community.getQuestionCount(), // ✅ Added question count
            universityName
        );
    }

    public CommunityDetailDto toDetailDto(Community community, User currentUser) {
        if (community == null) {
            return null;
        }

        boolean isMember = false;
        boolean isModerator = false;

        // Check the current user's status within the community
        if (currentUser != null && community.getMembers() != null) {
            for (CommunityMembership membership : community.getMembers()) {
                if (membership.getUser().getUserId().equals(currentUser.getUserId())) {
                    isMember = true;
                    if (membership.getRole() == CommunityRole.MODERATOR) {
                        isModerator = true;
                    }
                    break;
                }
            }
        }
        
        String universityName = (community.getUniversity() != null) ? community.getUniversity().getName() : null;
        UserSummaryDto creatorSummary = userMapper.toUserSummaryDto(community.getCreator());

        return new CommunityDetailDto(
            community.getCommunityId(),
            community.getName(),
            community.getDescription(),
            community.getMemberCount(),
            community.getQuestionCount(),
            community.getCreatedAt(),
            creatorSummary,
            universityName,
            isMember,
            isModerator
        );
    }

    public List<CommunitySummaryDto> toSummaryDtoList(List<Community> communities) {
        return communities.stream()
            .map(this::toSummaryDto)
            .collect(Collectors.toList());
    }
}