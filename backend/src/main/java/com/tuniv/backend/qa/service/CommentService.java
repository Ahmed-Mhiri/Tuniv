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
import com.tuniv.backend.qa.dto.CommentCreateRequest;
import com.tuniv.backend.qa.dto.CommentResponseDto;
import com.tuniv.backend.qa.dto.CommentUpdateRequest; // <-- IMPORT ADDED
import com.tuniv.backend.qa.dto.VoteInfo;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.CommentRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final AttachmentService attachmentService;
    private final PostAuthorizationService postAuthorizationService;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;
    private final VoteRepository voteRepository; // ✨ INJECT THE CONSOLIDATED REPOSITORY

    @Transactional
    @CacheEvict(value = "questions", key = "#result.answer.question.id")
    public Comment createComment(
            Integer answerId,
            CommentCreateRequest request,
            UserDetailsImpl currentUser,
            List<MultipartFile> files) {

        boolean isBodyEmpty = request.body() == null || request.body().trim().isEmpty();
        boolean hasFiles = files != null && files.stream().anyMatch(f -> f.getSize() > 0);

        if (isBodyEmpty && !hasFiles) {
            throw new IllegalArgumentException("Cannot create an empty comment. Please provide text or attach a file.");
        }

        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Answer answer = answerRepository.findWithQuestionById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        Comment comment = new Comment();
        comment.setBody(request.body());
        comment.setAnswer(answer);
        comment.setAuthor(author);

        if (request.parentCommentId() != null) {
            Comment parent = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            comment.setParentComment(parent);
        }

        Comment savedComment = commentRepository.save(comment);
        attachmentService.saveAttachments(files, savedComment);
        
        return savedComment;
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#result.answer.question.id")
    public Comment updateComment(Integer commentId, CommentUpdateRequest request, List<MultipartFile> newFiles, UserDetailsImpl currentUser) {
        Comment comment = commentRepository.findWithParentsById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        
        postAuthorizationService.checkOwnership(comment, currentUser);
        comment.setBody(request.body());

        if (request.attachmentIdsToDelete() != null && !request.attachmentIdsToDelete().isEmpty()) {
            Set<Attachment> toDelete = comment.getAttachments().stream()
                    .filter(att -> request.attachmentIdsToDelete().contains(att.getAttachmentId()))
                    .collect(Collectors.toSet());

            attachmentService.deleteAttachments(toDelete);
            toDelete.forEach(comment::removeAttachment);
        }

        attachmentService.saveAttachments(newFiles, comment);
        return commentRepository.save(comment);
    }
    
    @Transactional
    public void deleteComment(Integer commentId, UserDetailsImpl currentUser) {
        Comment comment = commentRepository.findWithParentsById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        postAuthorizationService.checkOwnership(comment, currentUser);

        Integer questionId = comment.getAnswer().getQuestion().getId();
        Cache questionsCache = cacheManager.getCache("questions");
        if (questionsCache != null) {
            questionsCache.evict(questionId);
        }

        attachmentService.deleteAttachments(comment.getAttachments());
        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByAnswer(Integer answerId, UserDetailsImpl currentUser) {
        if (!answerRepository.existsById(answerId)) {
            throw new ResourceNotFoundException("Answer not found with id: " + answerId);
        }

        List<Comment> topLevelComments = commentRepository.findTopLevelByAnswerIdsWithDetails(Collections.singletonList(answerId));

        if (topLevelComments.isEmpty()) {
            return Collections.emptyList();
        }
        
        // ✅ UPDATED: The call to flatMap is now cleaner and error-free.
        List<Integer> allCommentIds = topLevelComments.stream()
                .flatMap(this::flattenComments)
                .map(Comment::getId)
                .collect(Collectors.toList());
        
        Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (currentUser != null && !allCommentIds.isEmpty()) {
            // ✅ Use the new repository and DTO for efficiency
            List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), allCommentIds);
            votes.forEach(v -> currentUserVotes.put(v.postId(), v.value()));
        }

        return topLevelComments.stream()
                .map(comment -> QAMapper.toCommentResponseDto(comment, currentUser, currentUserVotes))
                .collect(Collectors.toList());
    }

    /**
     * ✅ UPDATED: Recursively flattens a comment and its children into a single Stream.
     * This stream-native approach is cleaner and helps the Java compiler with type inference.
     */
    private Stream<Comment> flattenComments(Comment comment) {
        if (comment.getChildren() == null || comment.getChildren().isEmpty()) {
            return Stream.of(comment);
        }
        // Return a stream of the parent comment, concatenated with the flattened stream of its children
        return Stream.concat(
            Stream.of(comment),
            comment.getChildren().stream().flatMap(this::flattenComments)
        );
    }
}