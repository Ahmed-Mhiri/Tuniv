package com.tuniv.backend.community.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tuniv.backend.community.dto.CommunityBasicDto;
import com.tuniv.backend.community.dto.CommunityDetailDto;
import com.tuniv.backend.community.dto.CommunityStatsDto;
import com.tuniv.backend.community.dto.CommunitySummaryDto;
import com.tuniv.backend.community.dto.CommunityTopicStatsDto;
import com.tuniv.backend.community.dto.CommunityWithStatsDto;
import com.tuniv.backend.community.dto.TrendingCommunityDto;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.community.model.CommunityMembership;
import com.tuniv.backend.community.model.CommunityRole;
import com.tuniv.backend.qa.model.TopicType;
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
            community.getTopicCount(),
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
            community.getTopicCount(),
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
        topicTypeDistribution.put(TopicType.POST, statsDto.postTopics());

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

    // ✅ NEW: Convert to CommunityWithStatsDto (combined detail + stats)
    public CommunityWithStatsDto toCommunityWithStatsDto(Community community, CommunityTopicStatsDto statsDto, User currentUser) {
        CommunityDetailDto detailDto = toDetailDto(community, currentUser);
        CommunityStatsDto stats = toStatsDto(statsDto, community);

        return new CommunityWithStatsDto(
            detailDto,
            stats
        );
    }

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