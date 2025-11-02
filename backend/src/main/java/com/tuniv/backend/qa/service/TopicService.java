package com.tuniv.backend.qa.service;

import com.tuniv.backend.auth.service.PostAuthorizationService;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.community.repository.CommunityRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.*;
import com.tuniv.backend.qa.mapper.TopicMapper;
import com.tuniv.backend.qa.model.*;
import com.tuniv.backend.qa.repository.*;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final ReplyRepository replyRepository;
    private final TopicTagRepository topicTagRepository;
    private final AttachmentRepository attachmentRepository;
    private final VoteRepository voteRepository;
    private final ModuleRepository moduleRepository;
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final TagService tagService;
    private final TopicMapper topicMapper;
    private final PostAuthorizationService postAuthorizationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    @Cacheable(value = "topics", key = "#topicId")
    public TopicResponseDto getTopicById(Integer topicId, UserDetailsImpl currentUser) {
        log.info("Fetching topic with ID: {}", topicId);
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        // ✅ FIXED: Fetch related data from repositories instead of entity collections
        List<Reply> replies = replyRepository.findByTopicIdOrderByCreatedAtAsc(topicId);
        List<Tag> tags = topicTagRepository.findTagsByTopicId(topicId);
        List<Attachment> attachments = attachmentRepository.findByPost_Id(topicId);

        List<Integer> postIds = replies.stream().map(Reply::getId).collect(Collectors.toList());
        postIds.add(topicId);
        Map<Integer, String> currentUserVotes = (currentUser != null)
            ? voteRepository.findUserVoteStatusForPosts(currentUser.getId(), postIds)
            : Map.of();

        return topicMapper.toTopicResponseDto(topic, replies, tags, attachments, currentUserVotes);
    }

    @Transactional
    public TopicResponseDto createTopic(TopicCreateRequest request, UserDetailsImpl currentUser, List<MultipartFile> files) {
        validateTopicCreation(request);
        User author = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // ✅ FIXED: Build topic correctly with university context
        University universityContext = determineUniversityContext(request.moduleId(), request.communityId());
        Topic topic = new Topic(request.title(), request.body(), author, request.topicType(), universityContext);
        assignContainer(topic, request.moduleId(), request.communityId());
        
        Topic savedTopic = topicRepository.save(topic);
        
        // ✅ FIXED: Handle tags via the join table
        handleTopicTags(savedTopic, request.tags(), author);
        updateContainerTopicCount(topic.getModule(), topic.getCommunity(), 1);

        // attachmentService.saveAttachments(files, savedTopic); // This logic would go here
        // eventPublisher.publishEvent(new NewTopicEvent(savedTopic));

        log.info("Created new topic '{}' with ID: {}", savedTopic.getTitle(), savedTopic.getId());
        return getTopicById(savedTopic.getId(), currentUser);
    }

    @Transactional
    @CacheEvict(value = "topics", key = "#topicId")
    public TopicResponseDto updateTopic(Integer topicId, TopicUpdateRequest request, UserDetailsImpl currentUser) {
        log.info("Updating topic with ID: {}", topicId);
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        
        postAuthorizationService.checkOwnership(topic, currentUser);

        topic.setTitle(request.title());
        topic.setBody(request.body());
        topic.setEdited(true);
        topic.setEditedAt(Instant.now());
        
        User author = topic.getAuthor(); // Get author from the topic itself
        
        // ✅ FIXED: Tag and attachment logic updated
        handleTopicTags(topic, request.tags(), author);
        // handleAttachmentsUpdate(topic, request.attachmentIdsToDelete(), newFiles);
        
        topicRepository.save(topic);
        return getTopicById(topicId, currentUser);
    }

    @Transactional
    @CacheEvict(value = "topics", key = "#topicId")
    public TopicResponseDto markAsSolution(Integer topicId, Integer replyId, UserDetailsImpl currentUser) {
        log.info("User {} marking reply {} as solution for topic {}", currentUser.getUsername(), replyId, topicId);
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        validateSolutionMarking(topic, currentUser);
        
        if (topic.isSolved()) {
            unmarkSolutionInternal(topic);
        }

        Reply solutionReply = replyRepository.findById(replyId)
            .orElseThrow(() -> new ResourceNotFoundException("Reply to be marked as solution not found"));

        if (!solutionReply.getTopic().getId().equals(topicId)) {
            throw new IllegalArgumentException("Reply does not belong to the specified topic.");
        }

        topic.setAcceptedSolution(solutionReply);
        topic.setSolved(true);
        topic.setSolutionAwardedAt(Instant.now());
        solutionReply.setSolution(true);

        replyRepository.save(solutionReply);
        topicRepository.save(topic);
        
        // eventPublisher.publishEvent(new SolutionMarkedEvent(topic, solutionReply));
        return getTopicById(topicId, currentUser);
    }

    @Transactional
    @CacheEvict(value = "topics", key = "#topicId")
    public TopicResponseDto unmarkSolution(Integer topicId, UserDetailsImpl currentUser) {
        log.info("User {} unmarking solution for topic {}", currentUser.getUsername(), topicId);
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        validateSolutionUnmarking(topic, currentUser);
        
        unmarkSolutionInternal(topic);
        
        // eventPublisher.publishEvent(new SolutionUnmarkedEvent(topic));
        return getTopicById(topicId, currentUser);
    }

    @Transactional
    @CacheEvict(value = "topics", key = "#topicId")
    public void deleteTopic(Integer topicId, UserDetailsImpl currentUser) {
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
            
        postAuthorizationService.checkOwnership(topic, currentUser);

        // ✅ FIXED: Use soft delete for data integrity
        topic.softDelete("Deleted by author");
        topicRepository.save(topic);
        
        updateContainerTopicCount(topic.getModule(), topic.getCommunity(), -1);
        log.info("Soft-deleted topic with ID: {}", topicId);
    }

    // --- Private Helper Methods ---

    private void validateTopicCreation(TopicCreateRequest request) {
        if ((request.moduleId() == null && request.communityId() == null) ||
            (request.moduleId() != null && request.communityId() != null)) {
            throw new IllegalArgumentException("A topic must be posted in exactly one module OR one community.");
        }
    }
    
    private University determineUniversityContext(Integer moduleId, Integer communityId) {
        if (moduleId != null) {
            return moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"))
                .getUniversity();
        }
        return null; // Communities are not tied to a university in the current schema
    }

    private void assignContainer(Topic topic, Integer moduleId, Integer communityId) {
        if (moduleId != null) {
            topic.setModule(moduleRepository.getReferenceById(moduleId));
        } else if (communityId != null) {
            topic.setCommunity(communityRepository.getReferenceById(communityId));
        }
    }

    private void handleTopicTags(Topic topic, List<String> tagNames, User user) {
        topicTagRepository.deleteAllByTopic_Id(topic.getId());
        if (tagNames != null && !tagNames.isEmpty()) {
            Set<Tag> tags = tagService.findOrCreateTags(tagNames);
            List<TopicTag> topicTags = tags.stream()
                .map(tag -> new TopicTag(topic, tag, user))
                .collect(Collectors.toList());
            topicTagRepository.saveAll(topicTags);
        }
    }

    private void unmarkSolutionInternal(Topic topic) {
        Reply oldSolution = topic.getAcceptedSolution();
        if (oldSolution != null) {
            oldSolution.setSolution(false);
            replyRepository.save(oldSolution);
        }
        topic.setAcceptedSolution(null);
        topic.setSolved(false);
        topic.setSolutionAwardedAt(null);
        topicRepository.save(topic);
    }

    private void validateSolutionMarking(Topic topic, UserDetailsImpl currentUser) {
        if (topic.getTopicType() != TopicType.QUESTION) {
            throw new IllegalArgumentException("Only QUESTION topics can have accepted solutions.");
        }
        if (!topic.getAuthor().getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the author of the topic can mark a reply as the solution.");
        }
    }

    private void validateSolutionUnmarking(Topic topic, UserDetailsImpl currentUser) {
        if (!topic.getAuthor().getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the author of the topic can unmark the solution.");
        }
        if (topic.getAcceptedSolution() == null) {
            throw new IllegalArgumentException("This topic does not have an accepted solution to unmark.");
        }
    }

    private void updateContainerTopicCount(Module module, Community community, int delta) {
        if (module != null) {
            moduleRepository.updateTopicCount(module.getModuleId(), delta);
        } else if (community != null) {
            communityRepository.updateTopicCount(community.getCommunityId(), delta);
        }
    }
}