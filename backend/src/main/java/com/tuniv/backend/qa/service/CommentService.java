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
    // ✅ FIX: AttachmentRepository is no longer needed here.
    private final ApplicationEventPublisher eventPublisher;

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
        attachmentService.saveAttachments(files, savedComment);

        // ✅ FIX: No need to re-fetch the comment or manually build attachment maps.
        // The 'savedComment' object is sufficient.
        
        eventPublisher.publishEvent(new NewCommentEvent(this, savedComment));

        // ✅ FIX: Call the updated, simpler mapper with empty vote maps for the new comment.
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

        // Collect all comment IDs (including children) for bulk fetching votes
        List<Integer> allCommentIds = topLevelComments.stream()
                .flatMap(comment -> flattenComments(comment).stream())
                .map(Comment::getId) // ✅ FIX: Use getId()
                .collect(Collectors.toList());
        
        // ✅ FIX: The manual fetching of attachments is no longer needed. It's handled by JPA's relationships.

        // Fetch all votes for these comments in a single query
List<CommentVote> votes = allCommentIds.isEmpty() ? Collections.emptyList() : commentVoteRepository.findByCommentIdIn(allCommentIds);

        // Process votes into maps
        Map<Integer, Integer> scores = votes.stream()
                .collect(Collectors.groupingBy(
                        vote -> vote.getComment().getId(), // ✅ FIX: Use getId()
                        Collectors.summingInt(vote -> (int) vote.getValue())
                ));

        Map<Integer, Integer> currentUserVotes = votes.stream()
                .filter(vote -> currentUser != null && vote.getUser().getUserId().equals(currentUser.getId()))
                .collect(Collectors.toMap(
                        vote -> vote.getComment().getId(), // ✅ FIX: Use getId()
                        vote -> (int) vote.getValue()
                ));

        // ✅ FIX: Call the updated mapper inside the stream, without the attachment map.
        return topLevelComments.stream()
                .map(comment -> QAMapper.toCommentResponseDto(comment, currentUser, scores, currentUserVotes))
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