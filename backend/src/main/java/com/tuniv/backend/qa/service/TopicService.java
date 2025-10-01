package com.tuniv.backend.qa.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.auth.service.PostAuthorizationService;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.community.repository.CommunityRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewTopicEvent;
import com.tuniv.backend.notification.event.SolutionAcceptedEvent;
import com.tuniv.backend.notification.event.SolutionUnmarkedEvent;
import com.tuniv.backend.qa.dto.ReplyCreateRequest;
import com.tuniv.backend.qa.dto.TopicCreateRequest;
import com.tuniv.backend.qa.dto.TopicResponseDto;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.dto.TopicUpdateRequest;
import com.tuniv.backend.qa.dto.VoteInfo;
import com.tuniv.backend.qa.mapper.TopicMapper;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.Tag;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.qa.model.TopicType;
import com.tuniv.backend.qa.repository.AttachmentRepository;
import com.tuniv.backend.qa.repository.ReplyRepository;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.qa.specification.TopicSpecifications;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final ReplyRepository replyRepository;
    private final ModuleRepository moduleRepository;
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;
    private final PostAuthorizationService postAuthorizationService;
    private final VoteRepository voteRepository;
    private final TagService tagService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    @Cacheable(value = "topics", key = "#topicId")
    public TopicResponseDto getTopicById(Integer topicId, UserDetailsImpl currentUser) {
        Topic topic = topicRepository.findFullTreeById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        // ✅ PERFORMANCE: Single query to get all votes for the entire thread
        List<Integer> allPostIds = new ArrayList<>();
        allPostIds.add(topic.getId());
        topic.getReplies().forEach(reply -> {
            allPostIds.add(reply.getId());
            // Flatten nested replies for vote fetching
            flattenReplies(reply).forEach(nestedReply -> allPostIds.add(nestedReply.getId()));
        });

        Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (currentUser != null && !allPostIds.isEmpty()) {
            List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), allPostIds);
            currentUserVotes = votes.stream()
                    .collect(Collectors.toMap(VoteInfo::postId, VoteInfo::value));
        }

        return TopicMapper.buildTopicResponseDto(topic, currentUser, currentUserVotes);
    }

    // ✅ NEW: Optimized personalized feed using specifications
    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getPersonalizedFeedUsingSpecifications(
            List<Integer> userIds, List<Integer> communityIds, 
            List<Integer> tagIds, List<Integer> moduleIds, 
            Integer currentUserId, Pageable pageable) {
        
        long startTime = System.currentTimeMillis();
        
        // Build specification for personalized feed
        Specification<Topic> spec = TopicSpecifications.withPersonalizedFeed(
            userIds, communityIds, tagIds, moduleIds
        );
        
        // Execute query with specification
        Page<Topic> topics = topicRepository.findAll(spec, pageable);
        
        // Batch load additional data efficiently
        final Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (!topics.getContent().isEmpty()) {
            currentUserVotes.putAll(loadAdditionalDataBatch(topics.getContent(), currentUserId));
        }
        
        log.debug("Personalized feed generated in {}ms for {} topics", 
                System.currentTimeMillis() - startTime, topics.getNumberOfElements());
        
        return topics.map(topic -> convertToSummaryDto(topic, currentUserVotes));
    }

    // ✅ NEW: Advanced search with multiple criteria
    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> searchTopics(
            String searchTerm, 
            TopicType topicType,
            Boolean isSolved,
            Integer minScore,
            Integer minReplies,
            Instant createdAfter,
            List<Integer> communityIds,
            List<Integer> moduleIds,
            List<String> tagNames,
            Integer currentUserId,
            Pageable pageable) {
        
        // Build complex specification
        Specification<Topic> spec = buildSearchSpecification(
            searchTerm, topicType, isSolved, minScore, minReplies, 
            createdAfter, communityIds, moduleIds, tagNames
        );
        
        Page<Topic> topics = topicRepository.findAll(spec, pageable);
        
        // Batch load additional data
        final Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (!topics.getContent().isEmpty()) {
            currentUserVotes.putAll(loadAdditionalDataBatch(topics.getContent(), currentUserId));
        }
        
        return topics.map(topic -> convertToSummaryDto(topic, currentUserVotes));
    }

    // ✅ NEW: Build complex search specification
    private Specification<Topic> buildSearchSpecification(
            String searchTerm, TopicType topicType, Boolean isSolved,
            Integer minScore, Integer minReplies, Instant createdAfter,
            List<Integer> communityIds, List<Integer> moduleIds, List<String> tagNames) {
        
        List<Specification<Topic>> specs = new ArrayList<>();
        
        // Search in title or body
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            specs.add(TopicSpecifications.titleOrBodyContains(searchTerm));
        }
        
        // Topic type filter
        if (topicType != null) {
            specs.add(TopicSpecifications.hasTopicType(topicType));
        }
        
        // Solved status filter
        if (isSolved != null) {
            specs.add(TopicSpecifications.isSolved(isSolved));
        }
        
        // Minimum score filter
        if (minScore != null && minScore > 0) {
            specs.add(TopicSpecifications.withMinimumScore(minScore));
        }
        
        // Minimum replies filter
        if (minReplies != null && minReplies > 0) {
            specs.add(TopicSpecifications.withMinimumReplies(minReplies));
        }
        
        // Date filter
        if (createdAfter != null) {
            specs.add(TopicSpecifications.createdAfter(createdAfter));
        }
        
        // Community filter
        if (communityIds != null && !communityIds.isEmpty()) {
            specs.add(TopicSpecifications.inCommunities(communityIds));
        }
        
        // Module filter
        if (moduleIds != null && !moduleIds.isEmpty()) {
            specs.add(TopicSpecifications.inModules(moduleIds));
        }
        
        // Tag filter
        if (tagNames != null && !tagNames.isEmpty()) {
            specs.add(TopicSpecifications.hasTags(tagNames));
        }
        
        // Combine all specifications with AND
        return TopicSpecifications.combineAnd(specs.toArray(new Specification[0]));
    }

    // ✅ NEW: Get popular topics with specifications
    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getPopularTopics(
            int minScore, 
            int minReplies,
            Integer currentUserId,
            Pageable pageable) {
        
        Specification<Topic> spec = TopicSpecifications.popularTopics(minScore, minReplies);
        Page<Topic> topics = topicRepository.findAll(spec, pageable);
        
        final Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (!topics.getContent().isEmpty()) {
            currentUserVotes.putAll(loadAdditionalDataBatch(topics.getContent(), currentUserId));
        }
        
        return topics.map(topic -> convertToSummaryDto(topic, currentUserVotes));
    }

    // ✅ NEW: Get recent unsolved questions
    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getRecentUnsolvedQuestions(
            Integer currentUserId,
            Pageable pageable) {
        
        Specification<Topic> spec = TopicSpecifications.recentUnsolvedQuestions();
        Page<Topic> topics = topicRepository.findAll(spec, pageable);
        
        final Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (!topics.getContent().isEmpty()) {
            currentUserVotes.putAll(loadAdditionalDataBatch(topics.getContent(), currentUserId));
        }
        
        return topics.map(topic -> convertToSummaryDto(topic, currentUserVotes));
    }

    // ✅ NEW: Batch load additional data for performance
    private Map<Integer, Integer> loadAdditionalDataBatch(List<Topic> topics, Integer currentUserId) {
        List<Integer> topicIds = topics.stream()
                .map(Topic::getId)
                .collect(Collectors.toList());
        
        // Batch load attachments using optimized repository method
        var attachments = attachmentRepository.findByPostIdIn(topicIds);
        var attachmentsByTopicId = attachments.stream()
                .collect(Collectors.groupingBy(attachment -> attachment.getPost().getId()));
        
        // Batch load user votes if current user is provided
        Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (currentUserId != null) {
            var userVotes = voteRepository.findAllVotesForUserByPostIds(currentUserId, topicIds);
            currentUserVotes = userVotes.stream()
                    .collect(Collectors.toMap(
                        VoteInfo::postId,
                        VoteInfo::value
                    ));
        }
        
        // Attach attachments to topics
        topics.forEach(topic -> {
            var topicAttachments = attachmentsByTopicId.get(topic.getId());
            if (topicAttachments != null) {
                topic.getAttachments().addAll(topicAttachments);
            }
        });
        
        return currentUserVotes;
    }

    // ✅ NEW: Convert Topic to TopicSummaryDto
    private TopicSummaryDto convertToSummaryDto(Topic topic) {
        return convertToSummaryDto(topic, null);
    }

    // ✅ NEW: Convert Topic to TopicSummaryDto with vote status
    private TopicSummaryDto convertToSummaryDto(Topic topic, Map<Integer, Integer> currentUserVotes) {
        // Determine container ID and name
        Integer containerId = null;
        String containerName = null;
        
        if (topic.getCommunity() != null) {
            containerId = topic.getCommunity().getCommunityId();
            containerName = topic.getCommunity().getName();
        } else if (topic.getModule() != null) {
            containerId = topic.getModule().getModuleId();
            containerName = topic.getModule().getName();
        }
        
        // Get tags as string list
        List<String> tags = topic.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
        
        // Determine current user's vote status
        String currentUserVote = null;
        if (currentUserVotes != null) {
            Integer voteValue = currentUserVotes.get(topic.getId());
            if (voteValue != null) {
                currentUserVote = voteValue == 1 ? "UPVOTE" : voteValue == -1 ? "DOWNVOTE" : null;
            }
        }
        
        return new TopicSummaryDto(
            topic.getId(),
            topic.getTitle(),
            topic.getTopicType(),
            topic.getAuthor().getUserId(),
            topic.getAuthor().getUsername(),
            topic.getCreatedAt(),
            topic.getScore(),
            topic.getReplyCount(), // Using denormalized count
            currentUserVote,
            topic.isSolved(),
            containerId,
            containerName,
            tags
        );
    }

    @Transactional
    public TopicResponseDto createTopic(TopicCreateRequest request, UserDetailsImpl currentUser, List<MultipartFile> files) {
        // Validation
        if ((request.moduleId() == null && request.communityId() == null) || 
            (request.moduleId() != null && request.communityId() != null)) {
            throw new IllegalArgumentException("A topic must be posted in exactly one module OR one community.");
        }

        // ✅ ENHANCED: Validate that only QUESTION topics can have solutions
        if (request.topicType() == TopicType.POST && request.tags().stream().anyMatch(tag -> 
            tag.equalsIgnoreCase("solved") || tag.equalsIgnoreCase("solution"))) {
            throw new IllegalArgumentException("Solution-related tags are only allowed for QUESTION topics.");
        }

        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
        
        Set<Tag> tags = tagService.findOrCreateTags(request.tags());

        Topic topic = new Topic();
        topic.setTitle(request.title());
        topic.setBody(request.body());
        topic.setAuthor(author);
        topic.setTags(tags);
        topic.setTopicType(request.topicType());
        // ✅ ENHANCED: Initialize solution state and reply count
        topic.setSolved(false);
        topic.setAcceptedSolution(null);
        topic.setReplyCount(0); // Initialize denormalized count

        // Assign to container (module or community)
        if (request.moduleId() != null) {
            Module module = moduleRepository.findById(request.moduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Error: Module not found with id: " + request.moduleId()));
            topic.setModule(module);
            module.incrementTopicCount();
            moduleRepository.save(module);
        } else {
            Community community = communityRepository.findById(request.communityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Error: Community not found with id: " + request.communityId()));
            topic.setCommunity(community);
            community.incrementTopicCount();
            communityRepository.save(community);
        }
        
        Topic savedTopic = topicRepository.save(topic);
        attachmentService.saveAttachments(files, savedTopic);

        // Publish event for notifications
        eventPublisher.publishEvent(new NewTopicEvent(savedTopic));
        
        return this.getTopicById(savedTopic.getId(), currentUser);
    }

    @Transactional
    @CacheEvict(value = "topics", key = "#topicId")
    public TopicResponseDto updateTopic(Integer topicId, TopicUpdateRequest request, List<MultipartFile> newFiles, UserDetailsImpl currentUser) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        postAuthorizationService.checkOwnership(topic, currentUser);
        
        topic.setTitle(request.title());
        topic.setBody(request.body());
        
        // Update tags
        Set<Tag> newTags = tagService.findOrCreateTags(request.tags());
        topic.getTags().clear();
        topic.getTags().addAll(newTags);
        
        // Handle attachments
        if (request.attachmentIdsToDelete() != null && !request.attachmentIdsToDelete().isEmpty()) {
            Set<Attachment> toDelete = topic.getAttachments().stream()
                    .filter(att -> request.attachmentIdsToDelete().contains(att.getAttachmentId()))
                    .collect(Collectors.toSet());
            attachmentService.deleteAttachments(toDelete);
            toDelete.forEach(topic::removeAttachment);
        }
        
        attachmentService.saveAttachments(newFiles, topic);
        
        Topic updatedTopic = topicRepository.save(topic);
        return getTopicById(updatedTopic.getId(), currentUser);
    }

    @Transactional
    @CacheEvict(value = "topics", key = "#topicId")
    public TopicResponseDto addReply(Integer topicId, ReplyCreateRequest request, UserDetailsImpl currentUser, List<MultipartFile> files) {
        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Topic not found with id: " + topicId));
        
        Reply reply = new Reply();
        reply.setBody(request.body());
        reply.setTopic(topic); // This automatically updates replyCount via setter
        reply.setAuthor(author);
        
        // Handle parent reply for nested replies
        if (request.parentReplyId() != null) {
            Reply parentReply = replyRepository.findById(request.parentReplyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent reply not found"));
            reply.setParentReply(parentReply); // This handles replyCount updates
        }

        Reply savedReply = replyRepository.save(reply);
        attachmentService.saveAttachments(files, savedReply);

        return this.getTopicById(topicId, currentUser);
    }

    @Transactional
    @CacheEvict(value = "topics", key = "#topicId")
    public Topic markAsSolution(Integer topicId, Integer replyId, UserDetailsImpl currentUser) {
        Topic topic = topicRepository.findWithDetailsById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        // ✅ ENHANCED: Validate topic type
        if (topic.getTopicType() != TopicType.QUESTION) {
            throw new IllegalArgumentException("Only QUESTION topics can have accepted solutions.");
        }

        Reply solution = replyRepository.findWithTopicById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found with id: " + replyId));

        // ✅ ENHANCED: Validate reply belongs to the topic
        if (!solution.getTopic().getId().equals(topicId)) {
            throw new IllegalArgumentException("Reply does not belong to the specified topic.");
        }

        // ✅ ENHANCED: Validate reply is an answer (not a comment)
        if (solution.getParentReply() != null) {
            throw new IllegalArgumentException("Only top-level replies (answers) can be marked as solutions.");
        }

        User topicAuthor = topic.getAuthor();
        User solutionAuthor = solution.getAuthor();

        if (!topicAuthor.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the author of the topic can mark a reply as the solution.");
        }

        if (topicAuthor.getUserId().equals(solutionAuthor.getUserId())) {
            throw new IllegalArgumentException("You cannot mark your own reply as the solution.");
        }
        
        // ✅ ENHANCED: Clear any existing solution first
        if (topic.getAcceptedSolution() != null) {
            unmarkSolutionInternal(topic);
        }
        
        // Set as solution
        topic.setAcceptedSolution(solution);
        topic.setSolved(true);
        
        // Award reputation
        final int ACCEPT_REPLY_REP = 15;
        final int ACCEPTS_REPLY_REP = 2;
        
        solutionAuthor.setReputationScore(solutionAuthor.getReputationScore() + ACCEPT_REPLY_REP);
        topicAuthor.setReputationScore(topicAuthor.getReputationScore() + ACCEPTS_REPLY_REP);

        userRepository.save(solutionAuthor);
        userRepository.save(topicAuthor);

        Topic savedTopic = topicRepository.save(topic);
        
        // ✅ ENHANCED: Publish solution event
        eventPublisher.publishEvent(new SolutionAcceptedEvent(topic, solution, solutionAuthor));
        
        return savedTopic;
    }

    // ✅ NEW: Unmark solution
    @Transactional
    @CacheEvict(value = "topics", key = "#topicId")
    public Topic unmarkSolution(Integer topicId, UserDetailsImpl currentUser) {
        Topic topic = topicRepository.findWithDetailsById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        User topicAuthor = topic.getAuthor();
        if (!topicAuthor.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the author of the topic can unmark the solution.");
        }

        if (topic.getAcceptedSolution() == null) {
            throw new IllegalArgumentException("This topic does not have an accepted solution.");
        }

        User solutionAuthor = topic.getAcceptedSolution().getAuthor();
        unmarkSolutionInternal(topic);

        // ✅ ENHANCED: Remove reputation points
        final int ACCEPT_REPLY_REP = 15;
        final int ACCEPTS_REPLY_REP = 2;
        
        solutionAuthor.setReputationScore(Math.max(0, solutionAuthor.getReputationScore() - ACCEPT_REPLY_REP));
        topicAuthor.setReputationScore(Math.max(0, topicAuthor.getReputationScore() - ACCEPTS_REPLY_REP));

        userRepository.save(solutionAuthor);
        userRepository.save(topicAuthor);

        Topic savedTopic = topicRepository.save(topic);
        
        // ✅ ENHANCED: Publish solution unmarked event
        eventPublisher.publishEvent(new SolutionUnmarkedEvent(topic, topic.getAcceptedSolution(), solutionAuthor));
        
        return savedTopic;
    }

    // ✅ NEW: Check if a reply can be marked as solution
    @Transactional(readOnly = true)
    public boolean canMarkAsSolution(Integer topicId, Integer replyId, UserDetailsImpl currentUser) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found"));

        // Only question author can mark solutions
        boolean isQuestionAuthor = topic.getAuthor().getUserId().equals(currentUser.getId());
        
        // Only QUESTION topics can have solutions
        boolean isQuestionTopic = topic.getTopicType() == TopicType.QUESTION;
        
        // Only answers (not comments) can be marked as solutions
        boolean isAnswer = reply.getParentReply() == null;
        
        // Reply should belong to the topic
        boolean belongsToTopic = reply.getTopic().getId().equals(topicId);
        
        // Cannot mark own reply as solution
        boolean isOwnReply = reply.getAuthor().getUserId().equals(currentUser.getId());
        
        // Topic should not already be solved (unless we're changing solution)
        boolean isAlreadySolved = topic.isSolved();
        
        return isQuestionAuthor && isQuestionTopic && isAnswer && belongsToTopic && !isOwnReply && !isAlreadySolved;
    }

    // ✅ NEW: Get solved topics for a user
    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getSolvedTopicsByUser(Integer userId, Pageable pageable, UserDetailsImpl currentUser) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        Page<TopicSummaryDto> solvedTopics = topicRepository.findSolvedTopicsByUser(userId, currentUserId, pageable);
        
        return enrichSummariesWithTags(solvedTopics, pageable);
    }

    // ✅ NEW: Get topics with accepted solutions by a user
    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getTopicsWithUserSolutions(Integer userId, Pageable pageable, UserDetailsImpl currentUser) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        Page<TopicSummaryDto> solutionTopics = topicRepository.findTopicsWithUserSolutions(userId, currentUserId, pageable);
        
        return enrichSummariesWithTags(solutionTopics, pageable);
    }

    @Transactional
    @CacheEvict(value = "topics", key = "#topicId")
    public void deleteTopic(Integer topicId, UserDetailsImpl currentUser) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        postAuthorizationService.checkOwnership(topic, currentUser);
        
        // Update counts based on container
        if (topic.getModule() != null) {
            Module module = topic.getModule();
            module.decrementTopicCount();
            moduleRepository.save(module);
        } else if (topic.getCommunity() != null) {
            Community community = topic.getCommunity();
            community.decrementTopicCount();
            communityRepository.save(community);
        }
        
        topicRepository.delete(topic);
    }

    // ✅ ENHANCED: Internal helper to unmark solution
    private void unmarkSolutionInternal(Topic topic) {
        topic.setAcceptedSolution(null);
        topic.setSolved(false);
    }

    // ✅ PERFORMANCE: Helper method to flatten nested replies for efficient vote fetching
    private List<Reply> flattenReplies(Reply reply) {
        List<Reply> allReplies = new ArrayList<>();
        if (reply.getChildReplies() != null && !reply.getChildReplies().isEmpty()) {
            for (Reply child : reply.getChildReplies()) {
                allReplies.add(child);
                allReplies.addAll(flattenReplies(child));
            }
        }
        return allReplies;
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

    private Page<TopicSummaryDto> enrichSummariesWithTags(Page<TopicSummaryDto> summaryPage, Pageable pageable) {
        if (summaryPage.isEmpty()) {
            return summaryPage;
        }

        List<Integer> topicIds = summaryPage.getContent().stream()
                .map(TopicSummaryDto::id)
                .toList();

        // ✅ PERFORMANCE: Single query to fetch all tags for all topics
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

    // ✅ NEW: Performance monitoring
    public void logPerformanceMetrics() {
        long totalTopics = topicRepository.count();
        long solvedTopics = topicRepository.count(TopicSpecifications.isSolved(true));
        long recentTopics = topicRepository.count(TopicSpecifications.createdAfter(
            Instant.now().minusSeconds(24 * 60 * 60)
        ));
        
        log.info("Topic Metrics - Total: {}, Solved: {}, Recent (24h): {}", 
                 totalTopics, solvedTopics, recentTopics);
    }
}