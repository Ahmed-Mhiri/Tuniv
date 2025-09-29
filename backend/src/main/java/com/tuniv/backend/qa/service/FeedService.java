package com.tuniv.backend.qa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.follow.model.Follow;
import com.tuniv.backend.follow.repository.FollowRepository;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.model.Tag;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.qa.repository.TopicRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final TopicRepository topicRepository;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getPersonalizedFeed(UserDetailsImpl currentUser, Pageable pageable) {
        // 1. Fetch all of the user's follow relationships
        List<Follow> follows = followRepository.findAllByUser_UserId(currentUser.getId());

        if (follows.isEmpty()) {
            return Page.empty(pageable); // If the user follows nothing, their feed is empty
        }

        // 2. Separate the followed IDs into lists based on their type
        List<Integer> followedUserIds = new ArrayList<>();
        List<Integer> followedCommunityIds = new ArrayList<>();
        List<Integer> followedTagIds = new ArrayList<>();
        List<Integer> followedModuleIds = new ArrayList<>();

        for (Follow follow : follows) {
            switch (follow.getTargetType()) {
                case USER -> followedUserIds.add(follow.getTargetId());
                case COMMUNITY -> followedCommunityIds.add(follow.getTargetId());
                case TAG -> followedTagIds.add(follow.getTargetId());
                case MODULE -> followedModuleIds.add(follow.getTargetId());
            }
        }
        
        // 3. Call the new repository method to get the feed (now using TopicRepository)
        Page<TopicSummaryDto> summaryPage = topicRepository.findPersonalizedFeed(
            followedUserIds, followedCommunityIds, followedTagIds, followedModuleIds,
            currentUser.getId(), pageable
        );

        return enrichSummariesWithTags(summaryPage, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getTopicsByModule(Integer moduleId, Pageable pageable, UserDetailsImpl currentUser) {
        Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        Page<TopicSummaryDto> summaryPage = topicRepository.findTopicSummariesByModuleId(moduleId, currentUserId, pageable);
        return enrichSummariesWithTags(summaryPage, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getTopicsByCommunity(Integer communityId, Pageable pageable, UserDetailsImpl currentUser) {
        Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        Page<TopicSummaryDto> summaryPage = topicRepository.findTopicSummariesByCommunityId(communityId, currentUserId, pageable);
        return enrichSummariesWithTags(summaryPage, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getTopicsByTag(String tagName, Pageable pageable, UserDetailsImpl currentUser) {
        Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        Page<TopicSummaryDto> summaryPage = topicRepository.findTopicSummariesByTag(tagName.toLowerCase(), currentUserId, pageable);
        return enrichSummariesWithTags(summaryPage, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getPopularFeed(Pageable pageable, UserDetailsImpl currentUser) {
        Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        Page<TopicSummaryDto> summaryPage = topicRepository.findPopularTopicSummaries(currentUserId, pageable);
        return enrichSummariesWithTags(summaryPage, pageable);
    }

    private Page<TopicSummaryDto> enrichSummariesWithTags(Page<TopicSummaryDto> summaryPage, Pageable pageable) {
        if (summaryPage.isEmpty()) {
            return summaryPage;
        }

        List<Integer> topicIds = summaryPage.getContent().stream()
                .map(TopicSummaryDto::id)
                .toList();

        // ✅ UPDATED: Use TopicRepository instead of QuestionRepository
        List<Topic> topicsWithTags = topicRepository.findWithTagsByIdIn(topicIds);

        Map<Integer, List<String>> tagsMap = topicsWithTags.stream()
                .collect(Collectors.toMap(
                    Topic::getId,
                    t -> t.getTags().stream().map(Tag::getName).toList()
                ));

        List<TopicSummaryDto> enrichedSummaries = summaryPage.getContent().stream()
                .map(summary -> summary.withTags(tagsMap.getOrDefault(summary.id(), Collections.emptyList())))
                .toList();

        return new PageImpl<>(enrichedSummaries, pageable, summaryPage.getTotalElements());
    }
}