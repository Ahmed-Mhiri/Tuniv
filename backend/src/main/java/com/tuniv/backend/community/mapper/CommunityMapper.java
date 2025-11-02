package com.tuniv.backend.community.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tuniv.backend.community.dto.CommunityBasicDto;
import com.tuniv.backend.community.dto.CommunityDetailDto;
import com.tuniv.backend.community.dto.CommunityMembershipStatusDto;
import com.tuniv.backend.community.dto.CommunityStatsDto;
import com.tuniv.backend.community.dto.CommunitySummaryDto;
import com.tuniv.backend.community.dto.CommunityTopicStatsDto;
import com.tuniv.backend.community.dto.CommunityWithStatsDto;
import com.tuniv.backend.community.dto.ModerationStatusDto;
import com.tuniv.backend.community.dto.TrendingCommunityDto;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.community.model.CommunityMembership;
import com.tuniv.backend.qa.model.TopicType;
import com.tuniv.backend.user.dto.UserSummaryDto;
import com.tuniv.backend.user.mapper.UserMapper;

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
            community.getIconUrl(),
            community.getMemberCount(),
            community.getTopicCount(),
            universityName
        );
    }

    // ✅ UPDATED: New signature with membership and moderation status
    public CommunityDetailDto toDetailDto(Community community, CommunityMembership membership, ModerationStatusDto moderationStatus) {
        if (community == null) {
            return null;
        }

        // Build membership status from the provided data
        CommunityMembershipStatusDto statusDto = new CommunityMembershipStatusDto(
            membership != null,                      // isMember
            membership != null ? membership.getRole() : null, // role
            moderationStatus != null ? moderationStatus.isBanned() : false, // isBanned
            moderationStatus != null ? moderationStatus.muteUntil() : null, // muteUntil
            membership != null ? membership.isNotificationsEnabled() : false // notificationsEnabled
        );
        
        String universityName = (community.getUniversity() != null) ? community.getUniversity().getName() : null;
        
        // FIX 1: Use getCreator() instead of getCreatedBy()
        UserSummaryDto creatorSummary = userMapper.toUserSummaryDto(community.getCreator());

        return new CommunityDetailDto(
            community.getCommunityId(),
            community.getName(),
            community.getDescription(),
            community.getBannerUrl(),
            community.getIconUrl(),
            community.getMemberCount(),
            community.getFollowerCount(),
            community.getTopicCount(),
            community.getCreatedAt(),
            creatorSummary,
            universityName,
            community.isVerified(),
            new com.tuniv.backend.community.dto.CommunitySettingsDto(
                community.getJoinPolicy(),
                community.getPostingPolicy(),
                community.getVisibility(),
                community.isAllowExternalMembers()
            ),
            statusDto
        );
    }

    // ❌ DELETED: Legacy method that was calling repository
    // public CommunityDetailDto toDetailDto(Community community, User currentUser) {
    //     This method has been removed as it was calling the repository
    // }

    public List<CommunitySummaryDto> toSummaryDtoList(List<Community> communities) {
        return communities.stream()
            .map(this::toSummaryDto)
            .collect(Collectors.toList());
    }

    public CommunityBasicDto toBasicDto(Community community) {
        if (community == null) {
            return null;
        }
        
        return new CommunityBasicDto(
            community.getCommunityId(),
            community.getName(),
            community.getUniversity() != null ? community.getUniversity().getName() : null
        );
    }

    // ✅ NEW: Convert CommunityTopicStatsDto to CommunityStatsDto
    public CommunityStatsDto toStatsDto(CommunityTopicStatsDto statsDto, Community community) {
        if (statsDto == null || community == null) {
            return null;
        }

        Map<TopicType, Long> topicTypeDistribution = new HashMap<>();
        topicTypeDistribution.put(TopicType.QUESTION, statsDto.questionTopics());
        // FIX 2: Change TopicType.POST to TopicType.DISCUSSION
        topicTypeDistribution.put(TopicType.DISCUSSION, statsDto.postTopics());

        return new CommunityStatsDto(
            statsDto.communityId(),
            statsDto.communityName(),
            statsDto.totalTopics().intValue(),
            community.getMemberCount(),
            statsDto.solvedTopics().intValue(),
            topicTypeDistribution,
            statsDto.getSolutionRate(),
            statsDto.getQuestionPercentage(),
            statsDto.getPostPercentage()
        );
    }

    // ✅ UPDATED: Convert to CommunityWithStatsDto with new signature
    public CommunityWithStatsDto toCommunityWithStatsDto(Community community, CommunityTopicStatsDto statsDto, CommunityMembership membership, ModerationStatusDto moderationStatus) {
        CommunityDetailDto detailDto = toDetailDto(community, membership, moderationStatus);
        CommunityStatsDto stats = toStatsDto(statsDto, community);

        return new CommunityWithStatsDto(
            detailDto,
            stats
        );
    }

    // ❌ DELETED: Legacy method that was calling repository
    // public CommunityWithStatsDto toCommunityWithStatsDto(Community community, CommunityTopicStatsDto statsDto, User currentUser) {
    //     This method has been removed as it was calling the repository
    // }

    // ✅ NEW: Convert to TrendingCommunityDto
    public TrendingCommunityDto toTrendingCommunityDto(Community community, Long recentActivityCount) {
        if (community == null) {
            return null;
        }

        String universityName = (community.getUniversity() != null) ? community.getUniversity().getName() : null;

        return new TrendingCommunityDto(
            community.getCommunityId(),
            community.getName(),
            community.getMemberCount(),
            community.getTopicCount(),
            universityName,
            recentActivityCount != null ? recentActivityCount : 0L,
            calculateActivityScore(community, recentActivityCount)
        );
    }

    // ✅ NEW: Helper method to calculate activity score
    private Double calculateActivityScore(Community community, Long recentActivityCount) {
        if (recentActivityCount == null || community.getTopicCount() == 0) {
            return 0.0;
        }

        double activityRatio = (double) recentActivityCount / community.getTopicCount();
        double memberRatio = community.getMemberCount() > 0 ? 
            (double) recentActivityCount / community.getMemberCount() : 0;

        return (activityRatio * 0.6) + (memberRatio * 0.4);
    }
}