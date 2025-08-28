package com.tuniv.backend.qa.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.config.security.services.UserDetailsImpl; // <-- IMPORT ADDED
import com.tuniv.backend.qa.dto.CommentCreateRequest;
import com.tuniv.backend.qa.dto.CommentResponseDto;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.AttachmentRepository;
import com.tuniv.backend.qa.repository.CommentRepository;
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
    private final AttachmentRepository attachmentRepository;

    @Transactional
    @CacheEvict(value = "questions", allEntries = true)
    public CommentResponseDto createComment(
            Integer answerId,
            CommentCreateRequest request,
            UserDetailsImpl currentUser,
            List<MultipartFile> files) {
        
        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found"));

        Comment comment = new Comment();
        comment.setBody(request.body());
        comment.setAnswer(answer);
        comment.setAuthor(author);
        answer.getComments().add(comment);
        
        if (request.parentCommentId() != null) {
            Comment parent = commentRepository.findById(request.parentCommentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            comment.setParentComment(parent);
            parent.getChildren().add(comment);
        }

        Comment savedComment = commentRepository.save(comment);

        List<Attachment> savedAttachments = attachmentService.saveAttachments(files, savedComment.getCommentId(), "COMMENT");
        // FIX: Update the in-memory object with its new attachments before mapping
        savedComment.setAttachments(new HashSet<>(savedAttachments));
        
        Map<Integer, List<Attachment>> commentAttachments = savedAttachments.stream()
            .collect(Collectors.groupingBy(Attachment::getPostId));

        return QAMapper.toCommentResponseDto(savedComment, currentUser, commentAttachments);
    }
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByAnswer(Integer answerId, UserDetailsImpl currentUser) {
        if (!answerRepository.existsById(answerId)) {
            throw new ResourceNotFoundException("Answer not found with id: " + answerId);
        }
        
        // 1. Fetch top-level comments
        List<Comment> topLevelComments = commentRepository.findByAnswerAnswerIdAndParentCommentIsNullOrderByCreatedAtAsc(answerId);

        // 2. Collect all comment IDs in the entire thread
        List<Integer> allCommentIds = topLevelComments.stream()
                .flatMap(comment -> flattenComments(comment).stream())
                .map(Comment::getCommentId)
                .collect(Collectors.toList());

        // 3. Fetch all attachments for those comments in one query
        Map<Integer, List<Attachment>> commentAttachments = attachmentRepository.findAllByPostTypeAndPostIdIn("COMMENT", allCommentIds)
                .stream().collect(Collectors.groupingBy(Attachment::getPostId));

        // 4. Map the top-level comments, passing the attachment map to the mapper
        return topLevelComments.stream()
                .map(comment -> QAMapper.toCommentResponseDto(comment, currentUser, commentAttachments))
                .collect(Collectors.toList());
    }
    
    // Helper to recursively flatten a comment thread
    private List<Comment> flattenComments(Comment comment) {
        List<Comment> list = new ArrayList<>();
        list.add(comment);
        if (comment.getChildren() != null) {
            comment.getChildren().forEach(child -> list.addAll(flattenComments(child)));
        }
        return list;
    }
}