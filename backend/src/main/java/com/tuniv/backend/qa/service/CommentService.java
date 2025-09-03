package com.tuniv.backend.qa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewCommentEvent;
import com.tuniv.backend.qa.dto.CommentCreateRequest; // <-- IMPORT ADDED
import com.tuniv.backend.qa.dto.CommentResponseDto;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.CommentVote;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.AttachmentRepository;
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
    private final CommentVoteRepository commentVoteRepository; // <-- 1. Inject the repository for votes
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @CacheEvict(value = "questions", allEntries = true)
    public CommentResponseDto createComment(
            Integer answerId,
            CommentCreateRequest request,
            UserDetailsImpl currentUser,
            List<MultipartFile> files) {
        
        boolean isBodyEmpty = request.body() == null || request.body().trim().isEmpty();
        boolean hasFiles = files != null && !files.stream().allMatch(MultipartFile::isEmpty);

        if (isBodyEmpty && !hasFiles) {
            throw new IllegalArgumentException("Cannot create an empty comment. Please provide text or attach a file.");
        }

        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found"));

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
        attachmentService.saveAttachments(files, savedComment.getCommentId(), "COMMENT");
        
        Comment finalComment = commentRepository.findById(savedComment.getCommentId())
                .orElseThrow(() -> new ResourceNotFoundException("Failed to re-fetch comment"));
        
        Map<Integer, List<Attachment>> commentAttachments = finalComment.getAttachments().stream()
                .collect(Collectors.groupingBy(Attachment::getPostId));
        
        // This is a new comment, so its score is 0 and there are no votes
        Map<Integer, Integer> scores = Collections.emptyMap();
        Map<Integer, Integer> currentUserVotes = Collections.emptyMap();

        eventPublisher.publishEvent(new NewCommentEvent(this, finalComment));

        // <-- 2. Call the updated mapper with empty vote maps
        return QAMapper.toCommentResponseDto(finalComment, currentUser, commentAttachments, scores, currentUserVotes);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByAnswer(Integer answerId, UserDetailsImpl currentUser) {
        if (!answerRepository.existsById(answerId)) {
            throw new ResourceNotFoundException("Answer not found with id: " + answerId);
        }
        
        List<Comment> topLevelComments = commentRepository.findByAnswerAnswerIdAndParentCommentIsNullOrderByCreatedAtAsc(answerId);
        
        // <-- 3. Collect all comment IDs (including children) for bulk fetching
        List<Integer> allCommentIds = topLevelComments.stream()
                .flatMap(comment -> flattenComments(comment).stream())
                .map(Comment::getCommentId)
                .collect(Collectors.toList());
        
        Map<Integer, List<Attachment>> commentAttachments = attachmentRepository.findAllByPostTypeAndPostIdIn("COMMENT", allCommentIds)
                .stream().collect(Collectors.groupingBy(Attachment::getPostId));

        // <-- 4. Fetch all votes for these comments in a single query
        List<CommentVote> votes = allCommentIds.isEmpty() ? Collections.emptyList() : commentVoteRepository.findByCommentCommentIdIn(allCommentIds);

        // <-- 5. Process votes into maps
        Map<Integer, Integer> scores = votes.stream()
            .collect(Collectors.groupingBy(
                vote -> vote.getComment().getCommentId(),
                Collectors.summingInt(vote -> (int) vote.getValue())
            ));

        Map<Integer, Integer> currentUserVotes = votes.stream()
            .filter(vote -> currentUser != null && vote.getUser().getUserId().equals(currentUser.getId()))
            .collect(Collectors.toMap(
                vote -> vote.getComment().getCommentId(),
                vote -> (int) vote.getValue()
            ));
        
        // <-- 6. Call the updated mapper inside the stream
        return topLevelComments.stream()
                .map(comment -> QAMapper.toCommentResponseDto(comment, currentUser, commentAttachments, scores, currentUserVotes))
                .collect(Collectors.toList());
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