package com.tuniv.backend.qa.service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewAnswerEvent;
import com.tuniv.backend.notification.event.NewCommentEvent;
import com.tuniv.backend.qa.dto.ReplyCreateRequest;
import com.tuniv.backend.qa.dto.ReplyResponseDto;
import com.tuniv.backend.qa.dto.ReplyUpdateRequest;
import com.tuniv.backend.qa.mapper.TopicMapper;
import com.tuniv.backend.qa.model.*;
import com.tuniv.backend.qa.repository.AttachmentRepository;
import com.tuniv.backend.qa.repository.ReplyRepository;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyService {

    //<editor-fold desc="Dependencies">
    private final ReplyRepository replyRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final AttachmentRepository attachmentRepository;
    private final PostAuthorizationService postAuthorizationService;
    private final ApplicationEventPublisher eventPublisher;
    private final TopicMapper topicMapper;
    private final CacheManager cacheManager;
    // private final AttachmentService attachmentService; // For file storage operations
    //</editor-fold>

    @Transactional
    @CacheEvict(value = "topics", key = "#topicId")
    public ReplyResponseDto createReply(Integer topicId, ReplyCreateRequest request, UserDetailsImpl currentUser, List<MultipartFile> files) {
        validateReplyContent(request, files);
        User author = userRepository.findById(currentUser.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Topic topic = topicRepository.findById(topicId).orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        
        validateReplyType(topic, request.parentReplyId());

        Reply reply = new Reply(request.body(), author, topic, topic.getUniversityContext());
        setParentReplyIfNeeded(reply, request.parentReplyId(), topicId);
        
        Reply savedReply = replyRepository.save(reply);

        topicRepository.incrementReplyCount(topic.getId());
        userRepository.incrementReplyCount(author.getUserId());

        // attachmentService.saveAttachments(files, savedReply);
        publishReplyEvent(savedReply);
        
        log.info("User {} created reply {} for topic {}", author.getUsername(), savedReply.getId(), topicId);
        return convertToSingleDto(savedReply, currentUser);
    }

    @Transactional
    @CacheEvict(value = "topics", key = "#result.topicId()")
    public ReplyResponseDto updateReply(Integer replyId, ReplyUpdateRequest request, UserDetailsImpl currentUser, List<MultipartFile> newFiles) {
        Reply reply = replyRepository.findById(replyId).orElseThrow(() -> new ResourceNotFoundException("Reply not found"));
        postAuthorizationService.checkOwnership(reply, currentUser);
        
        reply.setBody(request.body());
        reply.setEdited(true);
        reply.setEditedAt(Instant.now());
        
        updateAttachments(reply, request.attachmentIdsToDelete(), newFiles);
        
        Reply updatedReply = replyRepository.save(reply);
        return convertToSingleDto(updatedReply, currentUser);
    }
    
    @Transactional
    public void deleteReply(Integer replyId, UserDetailsImpl currentUser) {
        Reply reply = replyRepository.findById(replyId).orElseThrow(() -> new ResourceNotFoundException("Reply not found"));
        postAuthorizationService.checkOwnership(reply, currentUser);
        validateReplyDeletion(reply);
        
        reply.softDelete("Deleted by author");
        replyRepository.save(reply);

        topicRepository.decrementReplyCount(reply.getTopic().getId());
        userRepository.decrementReplyCount(reply.getAuthor().getUserId());

        // attachmentService.deleteAttachmentsForPost(reply);
        evictTopicCache(reply.getTopic().getId());
        log.info("Soft-deleted reply {} by user {}", replyId, currentUser.getUsername());
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getRepliesByTopic(Integer topicId, UserDetailsImpl currentUser) {
        if (!topicRepository.existsById(topicId)) throw new ResourceNotFoundException("Topic not found");
        List<Reply> replies = replyRepository.findByTopicIdOrderByCreatedAtAsc(topicId);
        return convertRepliesToTreeDto(replies, currentUser);
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getAnswersByTopic(Integer topicId, UserDetailsImpl currentUser) {
        Topic topic = topicRepository.findById(topicId).orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        if (topic.getTopicType() != TopicType.QUESTION) throw new IllegalArgumentException("Only QUESTION topics can have answers.");
        
        List<Reply> answers = replyRepository.findByTopicIdAndParentReplyIsNull(topicId);
        return convertRepliesToFlatDto(answers, currentUser);
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getCommentsByTopic(Integer topicId, UserDetailsImpl currentUser) {
        if (!topicRepository.existsById(topicId)) throw new ResourceNotFoundException("Topic not found");
        List<Reply> comments = replyRepository.findByTopicIdAndParentReplyIsNotNull(topicId);
        return convertRepliesToFlatDto(comments, currentUser);
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getNestedComments(Integer parentReplyId, UserDetailsImpl currentUser) {
        if (!replyRepository.existsById(parentReplyId)) throw new ResourceNotFoundException("Parent reply not found");
        List<Reply> nestedReplies = replyRepository.findByParentReply_IdOrderByCreatedAtAsc(parentReplyId);
        return convertRepliesToFlatDto(nestedReplies, currentUser);
    }

    @Transactional(readOnly = true)
    public boolean canMarkAsSolution(Integer replyId, UserDetailsImpl currentUser) {
        Reply reply = replyRepository.findById(replyId).orElseThrow(() -> new ResourceNotFoundException("Reply not found"));
        Topic topic = reply.getTopic();
        boolean isTopicAuthor = topic.getAuthor().getUserId().equals(currentUser.getId());
        boolean isOwnReply = reply.getAuthor().getUserId().equals(currentUser.getId());
        
        return isTopicAuthor && isAnswer(reply) && !isOwnReply && !topic.isSolved();
    }

    //<editor-fold desc="Private Helper Methods">
    private void validateReplyContent(ReplyCreateRequest request, List<MultipartFile> files) {
        boolean isBodyEmpty = request.body() == null || request.body().trim().isEmpty();
        boolean hasFiles = files != null && !files.isEmpty() && files.get(0).getSize() > 0;
        if (isBodyEmpty && !hasFiles) {
            throw new IllegalArgumentException("Cannot create an empty reply.");
        }
    }

    private Reply buildReplyEntity(ReplyCreateRequest request, Topic topic, User author) {
        // This helper is now replaced by the direct constructor call in `createReply`
        // but is kept here for structural reference.
        return new Reply(request.body(), author, topic, topic.getUniversityContext());
    }

    private void setParentReplyIfNeeded(Reply reply, Integer parentReplyId, Integer topicId) {
        if (parentReplyId != null) {
            Reply parent = replyRepository.findById(parentReplyId).orElseThrow(() -> new ResourceNotFoundException("Parent reply not found"));
            if (!parent.getTopic().getId().equals(topicId)) {
                throw new IllegalArgumentException("Parent reply does not belong to the same topic.");
            }
            reply.setParentReply(parent);
            reply.setDepth(parent.getDepth() + 1);
        }
    }

    private void updateAttachments(Post post, List<Integer> idsToDelete, List<MultipartFile> newFiles) {
        if (idsToDelete != null && !idsToDelete.isEmpty()) {
            List<Attachment> toDelete = attachmentRepository.findAllById(idsToDelete);
            // Add authorization check to ensure attachments belong to the post
            attachmentRepository.deleteAll(toDelete);
            // attachmentService.deleteFiles(toDelete);
        }
        // attachmentService.saveAttachments(newFiles, post);
    }

    private void validateReplyDeletion(Reply reply) {
        if (reply.isSolution()) {
            throw new IllegalArgumentException("Cannot delete a reply marked as the solution. Unmark it first.");
        }
        if (replyRepository.existsByParentReply_Id(reply.getId())) {
             throw new IllegalArgumentException("Cannot delete a reply that has other comments. Delete the comments first.");
        }
    }

    private void evictTopicCache(Integer topicId) {
        Cache topicsCache = cacheManager.getCache("topics");
        if (topicsCache != null) {
            topicsCache.evict(topicId);
        }
    }

    private boolean isAnswer(Reply reply) {
        return reply.getTopic().getTopicType() == TopicType.QUESTION && reply.getParentReply() == null;
    }

    private boolean isAcceptedSolution(Reply reply) {
        Topic topic = reply.getTopic();
        return topic.getAcceptedSolution() != null && topic.getAcceptedSolution().getId().equals(reply.getId());
    }

    private void publishReplyEvent(Reply reply) {
        if (isAnswer(reply)) {
            eventPublisher.publishEvent(new NewAnswerEvent(this, reply));
        } else {
            eventPublisher.publishEvent(new NewCommentEvent(this, reply));
        }
    }

    private ReplyResponseDto convertToSingleDto(Reply reply, UserDetailsImpl currentUser) {
        Map<Integer, String> vote = (currentUser != null) 
            ? voteRepository.findUserVoteStatusForPosts(currentUser.getId(), List.of(reply.getId()))
            : Collections.emptyMap();
        List<Attachment> attachments = attachmentRepository.findByPost_Id(reply.getId());
        return topicMapper.toReplyResponseDto(reply, vote, attachments);
    }

    private List<ReplyResponseDto> convertRepliesToFlatDto(List<Reply> replies, UserDetailsImpl currentUser) {
        if (replies.isEmpty()) return Collections.emptyList();
        List<Integer> replyIds = replies.stream().map(Reply::getId).collect(Collectors.toList());
        Map<Integer, String> currentUserVotes = (currentUser != null) ? voteRepository.findUserVoteStatusForPosts(currentUser.getId(), replyIds) : Collections.emptyMap();
        Map<Integer, List<Attachment>> attachmentsByPostId = loadAttachmentsBatch(replyIds);

        return replies.stream()
            .map(reply -> topicMapper.toReplyResponseDto(reply, currentUserVotes, attachmentsByPostId.getOrDefault(reply.getId(), Collections.emptyList())))
            .collect(Collectors.toList());
    }

    private List<ReplyResponseDto> convertRepliesToTreeDto(List<Reply> replies, UserDetailsImpl currentUser) {
        if (replies.isEmpty()) return Collections.emptyList();
        List<Integer> replyIds = replies.stream().map(Reply::getId).collect(Collectors.toList());
        Map<Integer, String> currentUserVotes = (currentUser != null) ? voteRepository.findUserVoteStatusForPosts(currentUser.getId(), replyIds) : Collections.emptyMap();
        Map<Integer, List<Attachment>> attachmentsByPostId = loadAttachmentsBatch(replyIds);

        return topicMapper.buildReplyTree(replies, currentUserVotes, attachmentsByPostId);
    }

    private Map<Integer, List<Attachment>> loadAttachmentsBatch(List<Integer> postIds) {
        if (postIds.isEmpty()) return Collections.emptyMap();
        List<Attachment> attachments = attachmentRepository.findByPost_IdIn(postIds);
        return attachments.stream().collect(Collectors.groupingBy(att -> att.getPost().getId()));
    }

    private void validateReplyType(Topic topic, Integer parentReplyId) {
        if (topic.getTopicType() == TopicType.QUESTION && parentReplyId != null) {
            Reply parent = replyRepository.getReferenceById(parentReplyId);
            if (parent.getParentReply() != null) {
                throw new IllegalArgumentException("Cannot nest comments more than one level deep in QUESTION topics.");
            }
        }
    }
    //</editor-fold>
}