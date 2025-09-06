package com.tuniv.backend.qa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.auth.service.PostAuthorizationService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.CommentCreateRequest;
import com.tuniv.backend.qa.dto.CommentResponseDto; // <-- IMPORT ADDED
import com.tuniv.backend.qa.dto.CommentUpdateRequest;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.CommentVote;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.CommentRepository;
import com.tuniv.backend.qa.repository.CommentVoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final AttachmentService attachmentService;
    private final ApplicationEventPublisher eventPublisher;
    private final PostAuthorizationService postAuthorizationService;

    @Transactional
    @CacheEvict(value = "questions", allEntries = true)
    public CommentResponseDto createComment(
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
        Answer answer = answerRepository.findById(answerId)
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

        // eventPublisher.publishEvent(new NewCommentEvent(this, savedComment)); // Uncomment if you have this event

        return QAMapper.toCommentResponseDto(savedComment, currentUser, Collections.emptyMap(), Collections.emptyMap());
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByAnswer(Integer answerId, UserDetailsImpl currentUser) {
        if (!answerRepository.existsById(answerId)) {
            throw new ResourceNotFoundException("Answer not found with id: " + answerId);
        }

        List<Comment> topLevelComments = commentRepository.findByAnswerIdAndParentCommentIsNullOrderByCreatedAtAsc(answerId);

        if (topLevelComments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> allCommentIds = topLevelComments.stream()
                .flatMap(comment -> flattenComments(comment).stream())
                .map(Comment::getId)
                .collect(Collectors.toList());
        
        List<CommentVote> votes = allCommentIds.isEmpty() ? Collections.emptyList() : commentVoteRepository.findByCommentIdIn(allCommentIds);

        Map<Integer, Integer> scores = votes.stream()
                .collect(Collectors.groupingBy(
                        vote -> vote.getComment().getId(),
                        Collectors.summingInt(vote -> (int) vote.getValue())
                ));

        Map<Integer, Integer> currentUserVotes = votes.stream()
                .filter(vote -> currentUser != null && vote.getUser().getUserId().equals(currentUser.getId()))
                .collect(Collectors.toMap(
                        vote -> vote.getComment().getId(),
                        vote -> (int) vote.getValue()
                ));

        return topLevelComments.stream()
                .map(comment -> QAMapper.toCommentResponseDto(comment, currentUser, scores, currentUserVotes))
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "questions", allEntries = true)
    public CommentResponseDto updateComment(Integer commentId, CommentUpdateRequest request, List<MultipartFile> newFiles, UserDetailsImpl currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        
        postAuthorizationService.checkOwnership(comment, currentUser);

        comment.setBody(request.body());

        // ✨ --- REFACTORED DELETION LOGIC --- ✨
        if (request.attachmentIdsToDelete() != null && !request.attachmentIdsToDelete().isEmpty()) {
            Set<Attachment> toDelete = comment.getAttachments().stream()
                    .filter(att -> request.attachmentIdsToDelete().contains(att.getAttachmentId()))
                    .collect(Collectors.toSet());

            // Delete physical files first
            attachmentService.deleteAttachments(toDelete);

            // Use the helper method to ensure both sides of the relationship are updated in memory
            toDelete.forEach(comment::removeAttachment);
        }

        // Add new attachments using the helper method inside this service
        attachmentService.saveAttachments(newFiles, comment);

        Comment updatedComment = commentRepository.save(comment);

        return QAMapper.toCommentResponseDto(updatedComment, currentUser, Collections.emptyMap(), Collections.emptyMap());
    }
    
    @Transactional
    @CacheEvict(value = "questions", allEntries = true)
    public void deleteComment(Integer commentId, UserDetailsImpl currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        postAuthorizationService.checkOwnership(comment, currentUser);

        // We must trigger the physical file cleanup before the entity is deleted.
        attachmentService.deleteAttachments(comment.getAttachments());

        commentRepository.delete(comment);
    }

    private List<Comment> flattenComments(Comment comment) {
        List<Comment> list = new ArrayList<>();
        list.add(comment);
        if (comment.getChildren() != null) {
            comment.getChildren().forEach(child -> list.addAll(flattenComments(child)));
        }
        return list;
    }
}