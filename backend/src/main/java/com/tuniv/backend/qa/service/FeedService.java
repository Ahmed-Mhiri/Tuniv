package com.tuniv.backend.qa.service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.mapper.TopicMapper;
import com.tuniv.backend.qa.model.Tag;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.qa.model.TopicTag;
import com.tuniv.backend.qa.model.TopicType;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.repository.TopicTagRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.qa.specification.TopicSpecifications;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    //<editor-fold desc="Dependencies">
    private final TopicRepository topicRepository;
    private final VoteRepository voteRepository;
    private final TopicTagRepository topicTagRepository;
    private final UserRepository userRepository;
    private final TopicMapper topicMapper;
    //</editor-fold>

    //<editor-fold desc="Public Feed Methods">
    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getPersonalizedFeed(
            List<Integer> userIds, List<Integer> communityIds,
            List<Integer> tagIds, List<Integer> moduleIds,
            UserDetailsImpl currentUser, Pageable pageable) {
        
        Specification<Topic> spec = TopicSpecifications.withPersonalizedFeed(userIds, communityIds, tagIds, moduleIds);
        return getFeed(spec, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> searchTopics(
            String searchTerm, TopicType topicType, Boolean isSolved,
            Integer minScore, Integer minReplies, Instant createdAfter,
            List<Integer> communityIds, List<Integer> moduleIds, List<String> tagNames,
            UserDetailsImpl currentUser, Pageable pageable) {
        
        Specification<Topic> spec = buildSearchSpecification(
            searchTerm, topicType, isSolved, minScore, minReplies,
            createdAfter, communityIds, moduleIds, tagNames
        );
        return getFeed(spec, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getPopularTopics(UserDetailsImpl currentUser, Pageable pageable) {
        Specification<Topic> spec = TopicSpecifications.popularTopics(10, 5); // Example thresholds
        return getFeed(spec, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getRecentUnsolvedQuestions(UserDetailsImpl currentUser, Pageable pageable) {
        Specification<Topic> spec = TopicSpecifications.recentUnsolvedQuestions();
        return getFeed(spec, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getSolvedTopicsByUser(Integer userId, Pageable pageable, UserDetailsImpl currentUser) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        Specification<Topic> spec = Specification.where(TopicSpecifications.byAuthor(userId)).and(TopicSpecifications.isSolved(true));
        return getFeed(spec, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getTopicsWithUserSolutions(Integer userId, Pageable pageable, UserDetailsImpl currentUser) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        Specification<Topic> spec = TopicSpecifications.withSolutionByAuthor(userId);
        return getFeed(spec, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getTopicsByModule(Integer moduleId, Pageable pageable, UserDetailsImpl currentUser) {
        Specification<Topic> spec = TopicSpecifications.inModules(Collections.singletonList(moduleId));
        return getFeed(spec, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getTopicsByCommunity(Integer communityId, Pageable pageable, UserDetailsImpl currentUser) {
        Specification<Topic> spec = TopicSpecifications.inCommunities(Collections.singletonList(communityId));
        return getFeed(spec, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getRecentRepliesFeed(UserDetailsImpl currentUser, Pageable pageable) {
        Specification<Topic> spec = TopicSpecifications.hasRecentActivity();
        return getFeed(spec, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getTrendingTopics(UserDetailsImpl currentUser, Pageable pageable) {
        Instant trendingSince = Instant.now().minusSeconds(7 * 24 * 60 * 60); // Last 7 days
        Specification<Topic> spec = TopicSpecifications.trendingTopics(trendingSince, 10, 5);
        return getFeed(spec, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getFastRisingTopics(UserDetailsImpl currentUser, Pageable pageable) {
        Specification<Topic> spec = TopicSpecifications.fastRisingTopics();
        return getFeed(spec, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getHighEngagementTopics(UserDetailsImpl currentUser, Pageable pageable) {
        Instant lastWeek = Instant.now().minusSeconds(7 * 24 * 60 * 60);
        Specification<Topic> spec = TopicSpecifications.trendingTopicsWithEngagement(lastWeek, 20); // min 20 total engagement
        return getFeed(spec, pageable, currentUser);
    }

    public void logPerformanceMetrics() {
        long totalTopics = topicRepository.count();
        long solvedTopics = topicRepository.count(TopicSpecifications.isSolved(true));
        long recentTopics = topicRepository.count(TopicSpecifications.createdAfter(Instant.now().minusSeconds(24 * 60 * 60)));
        log.info("Feed Metrics - Total Topics: {}, Solved: {}, Recent (24h): {}", totalTopics, solvedTopics, recentTopics);
    }
    //</editor-fold>

    //<editor-fold desc="Private Helper Methods">

    /**
     * The unified pattern for fetching and mapping any feed of topics.
     * All public methods now delegate to this central, performant logic.
     */
    private Page<TopicSummaryDto> getFeed(Specification<Topic> spec, Pageable pageable, UserDetailsImpl currentUser) {
        Page<Topic> topics = topicRepository.findAll(spec, pageable);
        if (topics.isEmpty()) {
            return Page.empty(pageable);
        }

        Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        Map<Integer, String> currentUserVotes = loadUserVotesForTopics(topics.getContent(), currentUserId);
        Map<Integer, List<Tag>> tagsByTopicId = loadTagsForTopics(topics.getContent());

        return topics.map(topic -> 
            topicMapper.toTopicSummaryDto(topic, tagsByTopicId.getOrDefault(topic.getId(), Collections.emptyList()), currentUserVotes.get(topic.getId()))
        );
    }
    
    private Specification<Topic> buildSearchSpecification(
            String searchTerm, TopicType topicType, Boolean isSolved,
            Integer minScore, Integer minReplies, Instant createdAfter,
            List<Integer> communityIds, List<Integer> moduleIds, List<String> tagNames) {
        
        List<Specification<Topic>> specs = new ArrayList<>();
        if (searchTerm != null && !searchTerm.trim().isEmpty()) specs.add(TopicSpecifications.titleOrBodyContains(searchTerm));
        if (topicType != null) specs.add(TopicSpecifications.hasTopicType(topicType));
        if (isSolved != null) specs.add(TopicSpecifications.isSolved(isSolved));
        if (minScore != null && minScore > 0) specs.add(TopicSpecifications.withMinimumScore(minScore));
        if (minReplies != null && minReplies > 0) specs.add(TopicSpecifications.withMinimumReplies(minReplies));
        if (createdAfter != null) specs.add(TopicSpecifications.createdAfter(createdAfter));
        if (communityIds != null && !communityIds.isEmpty()) specs.add(TopicSpecifications.inCommunities(communityIds));
        if (moduleIds != null && !moduleIds.isEmpty()) specs.add(TopicSpecifications.inModules(moduleIds));
        if (tagNames != null && !tagNames.isEmpty()) specs.add(TopicSpecifications.hasTags(tagNames));
        
        return TopicSpecifications.combineWithAnd(specs);
    }

    private Map<Integer, String> loadUserVotesForTopics(List<Topic> topics, Integer currentUserId) {
        if (currentUserId == null || topics.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Integer> topicIds = topics.stream().map(Topic::getId).collect(Collectors.toList());
        return voteRepository.findUserVoteStatusForPosts(currentUserId, topicIds);
    }

    private Map<Integer, List<Tag>> loadTagsForTopics(List<Topic> topics) {
        if (topics.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Integer> topicIds = topics.stream().map(Topic::getId).collect(Collectors.toList());
        List<TopicTag> topicTags = topicTagRepository.findByTopic_IdIn(topicIds);

        return topicTags.stream()
            .collect(Collectors.groupingBy(
                topicTag -> topicTag.getTopic().getId(),
                Collectors.mapping(TopicTag::getTag, Collectors.toList())
            ));
    }
    //</editor-fold>
}