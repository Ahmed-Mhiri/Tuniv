package com.tuniv.backend.qa.service;

import com.tuniv.backend.qa.model.TopicType;
import com.tuniv.backend.qa.repository.AttachmentRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.auth.service.PostAuthorizationService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewAnswerEvent;
import com.tuniv.backend.notification.event.NewCommentEvent;
import com.tuniv.backend.qa.dto.ReplyCreateRequest;
import com.tuniv.backend.qa.dto.ReplyResponseDto;
import com.tuniv.backend.qa.dto.ReplyUpdateRequest;
import com.tuniv.backend.qa.dto.VoteInfo;
import com.tuniv.backend.qa.mapper.TopicMapper;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.qa.repository.ReplyRepository;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyService {

    private final ReplyRepository replyRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;
    private final PostAuthorizationService postAuthorizationService;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;

    @Transactional
    @CacheEvict(value = "topics", key = "#result.topic.id")
    public Reply createReply(
            Integer topicId,
            ReplyCreateRequest request,
            UserDetailsImpl currentUser,
            List<MultipartFile> files) {

        boolean isBodyEmpty = request.body() == null || request.body().trim().isEmpty();
        boolean hasFiles = files != null && files.stream().anyMatch(f -> f.getSize() > 0);

        if (isBodyEmpty && !hasFiles) {
            throw new IllegalArgumentException("Cannot create an empty reply. Please provide text or attach a file.");
        }

        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        // ✅ ENHANCED: Validate reply type based on topic type and parent
        validateReplyType(topic, request.parentReplyId());

        Reply reply = new Reply();
        reply.setBody(request.body());
        reply.setTopic(topic); // This automatically updates replyCount via setter
        reply.setAuthor(author);

        // Handle nested replies
        if (request.parentReplyId() != null) {
            Reply parent = replyRepository.findById(request.parentReplyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent reply not found"));
            
            // ✅ ENHANCED: Validate parent reply belongs to same topic
            if (!parent.getTopic().getId().equals(topicId)) {
                throw new IllegalArgumentException("Parent reply does not belong to the same topic");
            }
            
            reply.setParentReply(parent); // This handles replyCount updates
        }

        Reply savedReply = replyRepository.save(reply);
        attachmentService.saveAttachments(files, savedReply);
        
        // ✅ ENHANCED: Publish appropriate event based on reply type
        publishReplyEvent(savedReply, author);
        
        return savedReply;
    }

    @Transactional
    @CacheEvict(value = "topics", key = "#result.topic.id")
    public Reply updateReply(Integer replyId, ReplyUpdateRequest request, List<MultipartFile> newFiles, UserDetailsImpl currentUser) {
        Reply reply = replyRepository.findWithTopicById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found with id: " + replyId));
        
        postAuthorizationService.checkOwnership(reply, currentUser);
        reply.setBody(request.body());

        if (request.attachmentIdsToDelete() != null && !request.attachmentIdsToDelete().isEmpty()) {
            Set<Attachment> toDelete = reply.getAttachments().stream()
                    .filter(att -> request.attachmentIdsToDelete().contains(att.getAttachmentId()))
                    .collect(Collectors.toSet());

            attachmentService.deleteAttachments(toDelete);
            toDelete.forEach(reply::removeAttachment);
        }

        attachmentService.saveAttachments(newFiles, reply);
        return replyRepository.save(reply);
    }
    
    @Transactional
    public void deleteReply(Integer replyId, UserDetailsImpl currentUser) {
        Reply reply = replyRepository.findWithTopicById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found with id: " + replyId));

        postAuthorizationService.checkOwnership(reply, currentUser);

        // ✅ ENHANCED: Check if this is an accepted solution before deletion
        if (isAcceptedSolution(reply)) {
            throw new IllegalArgumentException("Cannot delete a reply that is marked as the accepted solution. Please unmark it as solution first.");
        }

        // Evict cache
        Integer topicId = reply.getTopic().getId();
        Cache topicsCache = cacheManager.getCache("topics");
        if (topicsCache != null) {
            topicsCache.evict(topicId);
        }

        attachmentService.deleteAttachments(reply.getAttachments());
        replyRepository.delete(reply);
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getRepliesByTopic(Integer topicId, UserDetailsImpl currentUser) {
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic not found with id: " + topicId);
        }

        // ✅ PERFORMANCE: Fetch only top-level replies initially using EntityGraph
        List<Reply> topLevelReplies = replyRepository.findByTopicIdAndParentReplyIsNull(topicId);

        if (topLevelReplies.isEmpty()) {
            return Collections.emptyList();
        }
        
        // ✅ PERFORMANCE: Single query to get all votes for all replies in the thread
        List<Integer> allReplyIds = topLevelReplies.stream()
                .flatMap(this::flattenReplies)
                .map(Reply::getId)
                .collect(Collectors.toList());
        
        final Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (currentUser != null && !allReplyIds.isEmpty()) {
            List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), allReplyIds);
            votes.forEach(v -> currentUserVotes.put(v.postId(), v.value()));
        }

        return topLevelReplies.stream()
                .map(reply -> TopicMapper.toReplyResponseDto(reply, currentUser, currentUserVotes))
                .collect(Collectors.toList());
    }

    // ✅ NEW: Get only answers for a question topic with pagination
    @Transactional(readOnly = true)
    public Page<ReplyResponseDto> getAnswersByTopic(Integer topicId, UserDetailsImpl currentUser, Pageable pageable) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        // ✅ ENHANCED: Only QUESTION topics can have answers
        if (topic.getTopicType() != TopicType.QUESTION) {
            throw new IllegalArgumentException("Only question topics can have answers");
        }

        Page<Reply> answers = replyRepository.findByTopicIdAndParentReplyIsNullOrderByCreatedAtAsc(topicId, pageable);

        return convertRepliesToDtos(answers, currentUser);
    }

    // ✅ NEW: Get only comments for a topic with pagination
    @Transactional(readOnly = true)
    public Page<ReplyResponseDto> getCommentsByTopic(Integer topicId, UserDetailsImpl currentUser, Pageable pageable) {
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic not found with id: " + topicId);
        }

        Page<Reply> comments = replyRepository.findByTopicIdAndParentReplyIsNotNull(topicId, pageable);

        return convertRepliesToDtos(comments, currentUser);
    }

    // ✅ NEW: Get nested comments for a specific reply with pagination
    @Transactional(readOnly = true)
    public Page<ReplyResponseDto> getNestedComments(Integer parentReplyId, UserDetailsImpl currentUser, Pageable pageable) {
        Reply parentReply = replyRepository.findById(parentReplyId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent reply not found with id: " + parentReplyId));

        Page<Reply> nestedReplies = replyRepository.findByParentReplyIdOrderByCreatedAtAsc(parentReplyId, pageable);

        return convertRepliesToDtos(nestedReplies, currentUser);
    }

    // ✅ NEW: Get user's answers with pagination
    @Transactional(readOnly = true)
    public Page<ReplyResponseDto> getAnswersByUser(Integer userId, UserDetailsImpl currentUser, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Page<Reply> answers = replyRepository.findByAuthorUserIdAndTopicTopicTypeAndParentReplyIsNull(
            userId, TopicType.QUESTION, pageable
        );

        return convertRepliesToDtos(answers, currentUser);
    }

    // ✅ NEW: Get user's accepted solutions
    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getAcceptedSolutionsByUser(Integer userId, UserDetailsImpl currentUser) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<Reply> acceptedSolutions = replyRepository.findAcceptedSolutionsByUser(userId);

        return convertRepliesToDtos(acceptedSolutions, currentUser);
    }

    // ✅ NEW: Get recent replies with pagination
    @Transactional(readOnly = true)
    public Page<ReplyResponseDto> getRecentReplies(UserDetailsImpl currentUser, Pageable pageable) {
        Page<Reply> recentReplies = replyRepository.findByOrderByCreatedAtDesc(pageable);

        return convertRepliesToDtos(recentReplies, currentUser);
    }

    // ✅ NEW: Get popular replies (high score)
    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getPopularReplies(UserDetailsImpl currentUser) {
        List<Reply> popularReplies = replyRepository.findTop10ByOrderByScoreDesc();

        return convertRepliesToDtos(popularReplies, currentUser);
    }

    // ✅ NEW: Check if a user can mark a reply as solution
    @Transactional(readOnly = true)
    public boolean canMarkAsSolution(Integer replyId, UserDetailsImpl currentUser) {
        Reply reply = replyRepository.findWithTopicById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found with id: " + replyId));

        Topic topic = reply.getTopic();
        
        // Only question author can mark solutions
        boolean isQuestionAuthor = topic.getAuthor().getUserId().equals(currentUser.getId());
        
        // Only answers (not comments) can be marked as solutions
        boolean isAnswer = isAnswer(reply);
        
        // Reply should not be the author's own answer (optional business rule)
        boolean isOwnReply = reply.getAuthor().getUserId().equals(currentUser.getId());
        
        return isQuestionAuthor && isAnswer && !isOwnReply && !topic.isSolved();
    }

    // ✅ NEW: Batch operations for better performance
    @Transactional(readOnly = true)
    public Map<Integer, List<ReplyResponseDto>> getRepliesByMultipleTopics(List<Integer> topicIds, UserDetailsImpl currentUser) {
        if (topicIds == null || topicIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Batch fetch all replies for the given topics
        List<Reply> allReplies = replyRepository.findByTopicIdIn(topicIds);

        // Group by topic ID
        Map<Integer, List<Reply>> repliesByTopic = allReplies.stream()
                .collect(Collectors.groupingBy(reply -> reply.getTopic().getId()));

        // Convert to DTOs
        Map<Integer, List<ReplyResponseDto>> result = new HashMap<>();
        for (Map.Entry<Integer, List<Reply>> entry : repliesByTopic.entrySet()) {
            result.put(entry.getKey(), convertRepliesToDtos(entry.getValue(), currentUser));
        }

        return result;
    }

    // ✅ ENHANCED: Helper method to validate reply type based on topic and parent
    private void validateReplyType(Topic topic, Integer parentReplyId) {
        if (topic.getTopicType() == TopicType.QUESTION) {
            if (parentReplyId != null) {
                // This is a comment on an answer in a QUESTION topic
                Reply parent = replyRepository.findById(parentReplyId)
                        .orElseThrow(() -> new ResourceNotFoundException("Parent reply not found"));
                
                // Comments on QUESTION topics must be nested under answers
                if (parent.getParentReply() != null) {
                    throw new IllegalArgumentException("Cannot nest comments more than one level deep in question topics");
                }
            }
            // Top-level replies on QUESTION topics are answers (allowed)
        } else {
            // POST topics - any reply structure is allowed
            // No restrictions for POST topics
        }
    }

    // ✅ ENHANCED: Helper method to determine if a reply is an answer
    private boolean isAnswer(Reply reply) {
        return reply.getTopic().getTopicType() == TopicType.QUESTION && 
               reply.getParentReply() == null;
    }

    // ✅ ENHANCED: Helper method to determine if a reply is a comment
    private boolean isComment(Reply reply) {
        return reply.getTopic().getTopicType() == TopicType.POST || 
               reply.getParentReply() != null;
    }

    // ✅ ENHANCED: Helper method to check if reply is accepted solution
    private boolean isAcceptedSolution(Reply reply) {
        Topic topic = reply.getTopic();
        return topic.getAcceptedSolution() != null && 
               topic.getAcceptedSolution().getId().equals(reply.getId());
    }

    // ✅ ENHANCED: Publish appropriate event based on reply type
    private void publishReplyEvent(Reply reply, User author) {
        if (isAnswer(reply)) {
            eventPublisher.publishEvent(new NewAnswerEvent(reply, author));
        } else {
            eventPublisher.publishEvent(new NewCommentEvent(reply, author));
        }
    }

    // ✅ ENHANCED: Convert replies to DTOs with proper type handling (List version)
    private List<ReplyResponseDto> convertRepliesToDtos(List<Reply> replies, UserDetailsImpl currentUser) {
        if (replies.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> replyIds = replies.stream().map(Reply::getId).collect(Collectors.toList());
        
        final Map<Integer, Integer> currentUserVotes;
        if (currentUser != null && !replyIds.isEmpty()) {
            List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), replyIds);
            currentUserVotes = votes.stream()
                    .collect(Collectors.toMap(VoteInfo::postId, VoteInfo::value));
        } else {
            currentUserVotes = Collections.emptyMap();
        }

        return replies.stream()
                .map(reply -> TopicMapper.toReplyResponseDto(reply, currentUser, currentUserVotes))
                .collect(Collectors.toList());
    }

    // ✅ NEW: Convert replies to DTOs with proper type handling (Page version)
    private Page<ReplyResponseDto> convertRepliesToDtos(Page<Reply> replies, UserDetailsImpl currentUser) {
        if (replies.isEmpty()) {
            return Page.empty();
        }

        List<Integer> replyIds = replies.getContent().stream().map(Reply::getId).collect(Collectors.toList());
        
        final Map<Integer, Integer> currentUserVotes;
        if (currentUser != null && !replyIds.isEmpty()) {
            List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), replyIds);
            currentUserVotes = votes.stream()
                    .collect(Collectors.toMap(VoteInfo::postId, VoteInfo::value));
        } else {
            currentUserVotes = Collections.emptyMap();
        }

        return replies.map(reply -> TopicMapper.toReplyResponseDto(reply, currentUser, currentUserVotes));
    }

    /**
     * ✅ PERFORMANCE: Stream-based approach to flatten replies
     */
    private Stream<Reply> flattenReplies(Reply reply) {
        if (reply.getChildReplies() == null || reply.getChildReplies().isEmpty()) {
            return Stream.of(reply);
        }
        return Stream.concat(
            Stream.of(reply),
            reply.getChildReplies().stream().flatMap(this::flattenReplies)
        );
    }

    // ✅ NEW: Performance monitoring
    public void logPerformanceMetrics() {
        long totalReplies = replyRepository.count();
        long totalAnswers = replyRepository.countTopLevelRepliesByTopicId(1); // Example topic ID
        long totalComments = totalReplies - totalAnswers;
        long acceptedSolutions = replyRepository.countAcceptedSolutionsByUser(1); // Example user ID
        
        log.info("Reply Metrics - Total: {}, Answers: {}, Comments: {}, Accepted Solutions: {}", 
                 totalReplies, totalAnswers, totalComments, acceptedSolutions);
    }

    // ✅ NEW: Batch attachment loading
    private void loadAttachmentsBatch(List<Reply> replies) {
        if (replies.isEmpty()) {
            return;
        }

        List<Integer> replyIds = replies.stream()
                .map(Reply::getId)
                .collect(Collectors.toList());

        // Batch load attachments using optimized repository method
        var attachments = attachmentRepository.findByPostIdIn(replyIds);
        var attachmentsByReplyId = attachments.stream()
                .collect(Collectors.groupingBy(attachment -> attachment.getPost().getId()));

        // Attach attachments to replies
        replies.forEach(reply -> {
            var replyAttachments = attachmentsByReplyId.get(reply.getId());
            if (replyAttachments != null) {
                reply.getAttachments().addAll(replyAttachments);
            }
        });
    }
}package com.tuniv.backend.qa.service;

import com.tuniv.backend.qa.model.TopicType;
import com.tuniv.backend.qa.repository.AttachmentRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.auth.service.PostAuthorizationService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewAnswerEvent;
import com.tuniv.backend.notification.event.NewCommentEvent;
import com.tuniv.backend.qa.dto.ReplyCreateRequest;
import com.tuniv.backend.qa.dto.ReplyResponseDto;
import com.tuniv.backend.qa.dto.ReplyUpdateRequest;
import com.tuniv.backend.qa.dto.VoteInfo;
import com.tuniv.backend.qa.mapper.TopicMapper;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.qa.repository.ReplyRepository;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyService {

    private final ReplyRepository replyRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;
    private final PostAuthorizationService postAuthorizationService;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;

    @Transactional
    @CacheEvict(value = "topics", key = "#result.topic.id")
    public Reply createReply(
            Integer topicId,
            ReplyCreateRequest request,
            UserDetailsImpl currentUser,
            List<MultipartFile> files) {

        boolean isBodyEmpty = request.body() == null || request.body().trim().isEmpty();
        boolean hasFiles = files != null && files.stream().anyMatch(f -> f.getSize() > 0);

        if (isBodyEmpty && !hasFiles) {
            throw new IllegalArgumentException("Cannot create an empty reply. Please provide text or attach a file.");
        }

        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        // ✅ ENHANCED: Validate reply type based on topic type and parent
        validateReplyType(topic, request.parentReplyId());

        Reply reply = new Reply();
        reply.setBody(request.body());
        reply.setTopic(topic); // This automatically updates replyCount via setter
        reply.setAuthor(author);

        // Handle nested replies
        if (request.parentReplyId() != null) {
            Reply parent = replyRepository.findById(request.parentReplyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent reply not found"));
            
            // ✅ ENHANCED: Validate parent reply belongs to same topic
            if (!parent.getTopic().getId().equals(topicId)) {
                throw new IllegalArgumentException("Parent reply does not belong to the same topic");
            }
            
            reply.setParentReply(parent); // This handles replyCount updates
        }

        Reply savedReply = replyRepository.save(reply);
        attachmentService.saveAttachments(files, savedReply);
        
        // ✅ ENHANCED: Publish appropriate event based on reply type
        publishReplyEvent(savedReply, author);
        
        return savedReply;
    }

    @Transactional
    @CacheEvict(value = "topics", key = "#result.topic.id")
    public Reply updateReply(Integer replyId, ReplyUpdateRequest request, List<MultipartFile> newFiles, UserDetailsImpl currentUser) {
        Reply reply = replyRepository.findWithTopicById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found with id: " + replyId));
        
        postAuthorizationService.checkOwnership(reply, currentUser);
        reply.setBody(request.body());

        if (request.attachmentIdsToDelete() != null && !request.attachmentIdsToDelete().isEmpty()) {
            Set<Attachment> toDelete = reply.getAttachments().stream()
                    .filter(att -> request.attachmentIdsToDelete().contains(att.getAttachmentId()))
                    .collect(Collectors.toSet());

            attachmentService.deleteAttachments(toDelete);
            toDelete.forEach(reply::removeAttachment);
        }

        attachmentService.saveAttachments(newFiles, reply);
        return replyRepository.save(reply);
    }
    
    @Transactional
    public void deleteReply(Integer replyId, UserDetailsImpl currentUser) {
        Reply reply = replyRepository.findWithTopicById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found with id: " + replyId));

        postAuthorizationService.checkOwnership(reply, currentUser);

        // ✅ ENHANCED: Check if this is an accepted solution before deletion
        if (isAcceptedSolution(reply)) {
            throw new IllegalArgumentException("Cannot delete a reply that is marked as the accepted solution. Please unmark it as solution first.");
        }

        // Evict cache
        Integer topicId = reply.getTopic().getId();
        Cache topicsCache = cacheManager.getCache("topics");
        if (topicsCache != null) {
            topicsCache.evict(topicId);
        }

        attachmentService.deleteAttachments(reply.getAttachments());
        replyRepository.delete(reply);
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getRepliesByTopic(Integer topicId, UserDetailsImpl currentUser) {
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic not found with id: " + topicId);
        }

        // ✅ PERFORMANCE: Fetch only top-level replies initially using EntityGraph
        List<Reply> topLevelReplies = replyRepository.findByTopicIdAndParentReplyIsNull(topicId);

        if (topLevelReplies.isEmpty()) {
            return Collections.emptyList();
        }
        
        // ✅ PERFORMANCE: Single query to get all votes for all replies in the thread
        List<Integer> allReplyIds = topLevelReplies.stream()
                .flatMap(this::flattenReplies)
                .map(Reply::getId)
                .collect(Collectors.toList());
        
        final Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (currentUser != null && !allReplyIds.isEmpty()) {
            List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), allReplyIds);
            votes.forEach(v -> currentUserVotes.put(v.postId(), v.value()));
        }

        return topLevelReplies.stream()
                .map(reply -> TopicMapper.toReplyResponseDto(reply, currentUser, currentUserVotes))
                .collect(Collectors.toList());
    }

    // ✅ NEW: Get only answers for a question topic (using existing repository method)
    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getAnswersByTopic(Integer topicId, UserDetailsImpl currentUser) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        // ✅ ENHANCED: Only QUESTION topics can have answers
        if (topic.getTopicType() != TopicType.QUESTION) {
            throw new IllegalArgumentException("Only question topics can have answers");
        }

        List<Reply> answers = replyRepository.findAnswersByTopicId(topicId);

        return convertRepliesToDtos(answers, currentUser);
    }

    // ✅ NEW: Get only comments for a topic (using existing repository method)
    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getCommentsByTopic(Integer topicId, UserDetailsImpl currentUser) {
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic not found with id: " + topicId);
        }

        List<Reply> comments = replyRepository.findCommentsByTopicId(topicId);

        return convertRepliesToDtos(comments, currentUser);
    }

    // ✅ NEW: Get nested comments for a specific reply (using existing repository method)
    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getNestedComments(Integer parentReplyId, UserDetailsImpl currentUser) {
        Reply parentReply = replyRepository.findById(parentReplyId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent reply not found with id: " + parentReplyId));

        List<Reply> nestedReplies = replyRepository.findByParentReplyIdOrderByCreatedAtAsc(parentReplyId);

        return convertRepliesToDtos(nestedReplies, currentUser);
    }

    // ✅ NEW: Get user's answers (using custom query with pagination simulation)
    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getAnswersByUser(Integer userId, UserDetailsImpl currentUser) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<Reply> answers = replyRepository.findAnswersByUser(userId);

        return convertRepliesToDtos(answers, currentUser);
    }

    // ✅ NEW: Get user's accepted solutions
    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getAcceptedSolutionsByUser(Integer userId, UserDetailsImpl currentUser) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<Reply> acceptedSolutions = replyRepository.findAcceptedSolutionsByUser(userId);

        return convertRepliesToDtos(acceptedSolutions, currentUser);
    }

    // ✅ NEW: Get recent replies (using existing repository method)
    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getRecentReplies(UserDetailsImpl currentUser) {
        List<Reply> recentReplies = replyRepository.findTop10ByOrderByCreatedAtDesc();

        return convertRepliesToDtos(recentReplies, currentUser);
    }

    // ✅ NEW: Get popular replies (high score) - using existing repository method
    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getPopularReplies(UserDetailsImpl currentUser) {
        List<Reply> popularReplies = replyRepository.findTop10ByOrderByScoreDesc();

        return convertRepliesToDtos(popularReplies, currentUser);
    }

    // ✅ NEW: Check if a user can mark a reply as solution
    @Transactional(readOnly = true)
    public boolean canMarkAsSolution(Integer replyId, UserDetailsImpl currentUser) {
        Reply reply = replyRepository.findWithTopicById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found with id: " + replyId));

        Topic topic = reply.getTopic();
        
        // Only question author can mark solutions
        boolean isQuestionAuthor = topic.getAuthor().getUserId().equals(currentUser.getId());
        
        // Only answers (not comments) can be marked as solutions
        boolean isAnswer = isAnswer(reply);
        
        // Reply should not be the author's own answer (optional business rule)
        boolean isOwnReply = reply.getAuthor().getUserId().equals(currentUser.getId());
        
        return isQuestionAuthor && isAnswer && !isOwnReply && !topic.isSolved();
    }

    // ✅ NEW: Batch operations for better performance
    @Transactional(readOnly = true)
    public Map<Integer, List<ReplyResponseDto>> getRepliesByMultipleTopics(List<Integer> topicIds, UserDetailsImpl currentUser) {
        if (topicIds == null || topicIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Batch fetch all replies for the given topics
        List<Reply> allReplies = replyRepository.findByTopicIdIn(topicIds);

        // Group by topic ID
        Map<Integer, List<Reply>> repliesByTopic = allReplies.stream()
                .collect(Collectors.groupingBy(reply -> reply.getTopic().getId()));

        // Convert to DTOs
        Map<Integer, List<ReplyResponseDto>> result = new HashMap<>();
        for (Map.Entry<Integer, List<Reply>> entry : repliesByTopic.entrySet()) {
            result.put(entry.getKey(), convertRepliesToDtos(entry.getValue(), currentUser));
        }

        return result;
    }

    // ✅ NEW: Get replies with pagination (manual implementation since we don't have pagination in repository)
    @Transactional(readOnly = true)
    public Page<ReplyResponseDto> getRepliesByTopicWithPagination(Integer topicId, UserDetailsImpl currentUser, Pageable pageable) {
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic not found with id: " + topicId);
        }

        // Get all replies for the topic
        List<Reply> allReplies = replyRepository.findByTopicIdAndParentReplyIsNull(topicId);
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allReplies.size());
        
        if (start > allReplies.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, allReplies.size());
        }
        
        List<Reply> pagedReplies = allReplies.subList(start, end);
        List<ReplyResponseDto> replyDtos = convertRepliesToDtos(pagedReplies, currentUser);
        
        return new PageImpl<>(replyDtos, pageable, allReplies.size());
    }

    // ✅ ENHANCED: Helper method to validate reply type based on topic and parent
    private void validateReplyType(Topic topic, Integer parentReplyId) {
        if (topic.getTopicType() == TopicType.QUESTION) {
            if (parentReplyId != null) {
                // This is a comment on an answer in a QUESTION topic
                Reply parent = replyRepository.findById(parentReplyId)
                        .orElseThrow(() -> new ResourceNotFoundException("Parent reply not found"));
                
                // Comments on QUESTION topics must be nested under answers
                if (parent.getParentReply() != null) {
                    throw new IllegalArgumentException("Cannot nest comments more than one level deep in question topics");
                }
            }
            // Top-level replies on QUESTION topics are answers (allowed)
        } else {
            // POST topics - any reply structure is allowed
            // No restrictions for POST topics
        }
    }

    // ✅ ENHANCED: Helper method to determine if a reply is an answer
    private boolean isAnswer(Reply reply) {
        return reply.getTopic().getTopicType() == TopicType.QUESTION && 
               reply.getParentReply() == null;
    }

    // ✅ ENHANCED: Helper method to determine if a reply is a comment
    private boolean isComment(Reply reply) {
        return reply.getTopic().getTopicType() == TopicType.POST || 
               reply.getParentReply() != null;
    }

    // ✅ ENHANCED: Helper method to check if reply is accepted solution
    private boolean isAcceptedSolution(Reply reply) {
        Topic topic = reply.getTopic();
        return topic.getAcceptedSolution() != null && 
               topic.getAcceptedSolution().getId().equals(reply.getId());
    }

    // ✅ ENHANCED: Publish appropriate event based on reply type
    private void publishReplyEvent(Reply reply, User author) {
        if (isAnswer(reply)) {
            eventPublisher.publishEvent(new NewAnswerEvent(reply, author));
        } else {
            eventPublisher.publishEvent(new NewCommentEvent(reply, author));
        }
    }

    // ✅ ENHANCED: Convert replies to DTOs with proper type handling
    private List<ReplyResponseDto> convertRepliesToDtos(List<Reply> replies, UserDetailsImpl currentUser) {
        if (replies.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> replyIds = replies.stream().map(Reply::getId).collect(Collectors.toList());
        
        final Map<Integer, Integer> currentUserVotes;
        if (currentUser != null && !replyIds.isEmpty()) {
            List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), replyIds);
            currentUserVotes = votes.stream()
                    .collect(Collectors.toMap(VoteInfo::postId, VoteInfo::value));
        } else {
            currentUserVotes = Collections.emptyMap();
        }

        // Load attachments batch
        loadAttachmentsBatch(replies);

        return replies.stream()
                .map(reply -> TopicMapper.toReplyResponseDto(reply, currentUser, currentUserVotes))
                .collect(Collectors.toList());
    }

    /**
     * ✅ PERFORMANCE: Stream-based approach to flatten replies
     */
    private Stream<Reply> flattenReplies(Reply reply) {
        if (reply.getChildReplies() == null || reply.getChildReplies().isEmpty()) {
            return Stream.of(reply);
        }
        return Stream.concat(
            Stream.of(reply),
            reply.getChildReplies().stream().flatMap(this::flattenReplies)
        );
    }

    // ✅ NEW: Performance monitoring
    public void logPerformanceMetrics() {
        long totalReplies = replyRepository.count();
        long totalAnswers = replyRepository.countAnswersByUser(1); // Example user ID
        long totalComments = replyRepository.countCommentsByUser(1); // Example user ID
        long acceptedSolutions = replyRepository.countAcceptedSolutionsByUser(1); // Example user ID
        
        log.info("Reply Metrics - Total: {}, Answers: {}, Comments: {}, Accepted Solutions: {}", 
                 totalReplies, totalAnswers, totalComments, acceptedSolutions);
    }

    // ✅ NEW: Batch attachment loading
    private void loadAttachmentsBatch(List<Reply> replies) {
        if (replies.isEmpty()) {
            return;
        }

        List<Integer> replyIds = replies.stream()
                .map(Reply::getId)
                .collect(Collectors.toList());

        // Batch load attachments using optimized repository method
        var attachments = attachmentRepository.findByPostIdIn(replyIds);
        var attachmentsByReplyId = attachments.stream()
                .collect(Collectors.groupingBy(attachment -> attachment.getPost().getId()));

        // Attach attachments to replies
        replies.forEach(reply -> {
            var replyAttachments = attachmentsByReplyId.get(reply.getId());
            if (replyAttachments != null) {
                reply.getAttachments().addAll(replyAttachments);
            }
        });
    }

    // ✅ NEW: Check if user has replied to a topic
    @Transactional(readOnly = true)
    public boolean hasUserRepliedToTopic(Integer topicId, Integer userId) {
        return replyRepository.existsByTopicIdAndAuthorUserId(topicId, userId);
    }

    // ✅ NEW: Get user's reply to a specific topic
    @Transactional(readOnly = true)
    public Optional<ReplyResponseDto> getUserReplyToTopic(Integer topicId, Integer userId, UserDetailsImpl currentUser) {
        Optional<Reply> userReply = replyRepository.findByTopicIdAndAuthorUserId(topicId, userId);
        return userReply.map(reply -> {
            List<ReplyResponseDto> dtos = convertRepliesToDtos(List.of(reply), currentUser);
            return dtos.isEmpty() ? null : dtos.get(0);
        });
    }
}