package com.tuniv.backend.qa.service;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
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
import com.tuniv.backend.qa.model.TopicType;
import com.tuniv.backend.qa.repository.ReplyRepository;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class ReplyService {

    private final ReplyRepository replyRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final AttachmentService attachmentService;
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
        reply.setTopic(topic);
        reply.setAuthor(author);

        // Handle nested replies
        if (request.parentReplyId() != null) {
            Reply parent = replyRepository.findById(request.parentReplyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent reply not found"));
            
            // ✅ ENHANCED: Validate parent reply belongs to same topic
            if (!parent.getTopic().getId().equals(topicId)) {
                throw new IllegalArgumentException("Parent reply does not belong to the same topic");
            }
            
            reply.setParentReply(parent);
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

        // ✅ PERFORMANCE: Fetch only top-level replies initially
        List<Reply> topLevelReplies = replyRepository.findTopLevelByTopicIdWithDetails(topicId);

        if (topLevelReplies.isEmpty()) {
            return Collections.emptyList();
        }
        
        // ✅ PERFORMANCE: Single query to get all votes for all replies in the thread
        List<Integer> allReplyIds = topLevelReplies.stream()
                .flatMap(this::flattenReplies)
                .map(Reply::getId)
                .collect(Collectors.toList());
        
        Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (currentUser != null && !allReplyIds.isEmpty()) {
            List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), allReplyIds);
            votes.forEach(v -> currentUserVotes.put(v.postId(), v.value()));
        }

        return topLevelReplies.stream()
                .map(reply -> TopicMapper.toReplyResponseDto(reply, currentUser, currentUserVotes))
                .collect(Collectors.toList());
    }

    // ✅ NEW: Get only answers for a question topic
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

    // ✅ NEW: Get only comments for a topic
    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getCommentsByTopic(Integer topicId, UserDetailsImpl currentUser) {
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic not found with id: " + topicId);
        }

        List<Reply> comments = replyRepository.findCommentsByTopicId(topicId);

        return convertRepliesToDtos(comments, currentUser);
    }

    // ✅ NEW: Get nested comments for a specific reply
    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getNestedComments(Integer parentReplyId, UserDetailsImpl currentUser) {
        Reply parentReply = replyRepository.findById(parentReplyId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent reply not found with id: " + parentReplyId));

        List<Reply> nestedReplies = replyRepository.findByParentReplyIdOrderByCreatedAtAsc(parentReplyId);

        return convertRepliesToDtos(nestedReplies, currentUser);
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
}