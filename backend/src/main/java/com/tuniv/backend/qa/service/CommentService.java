package com.tuniv.backend.qa.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.CommentCreateRequest;
import com.tuniv.backend.qa.dto.CommentResponseDto;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.CommentRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException; // <-- IMPORT ADDED
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final AttachmentService attachmentService; // <-- DEPENDENCY ADDED

    // Method signature is updated to accept a list of files
    public CommentResponseDto createComment(
            Integer answerId,
            CommentCreateRequest request,
            UserDetailsImpl currentUser,
            List<MultipartFile> files) {
        
        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        Comment comment = new Comment();
        comment.setBody(request.body());
        comment.setAnswer(answer);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);

        // --- NEW FILE UPLOAD LOGIC ---
        // Call the dedicated service to handle attachments
        attachmentService.saveAttachments(files, savedComment.getCommentId(), "COMMENT");

        return mapToDto(savedComment);
    }

    public List<CommentResponseDto> getCommentsByAnswer(Integer answerId) {
        if (!answerRepository.existsById(answerId)) {
            throw new ResourceNotFoundException("Answer not found with id: " + answerId);
        }
        return commentRepository.findByAnswerAnswerIdOrderByCreatedAtAsc(answerId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private CommentResponseDto mapToDto(Comment comment) {
        return new CommentResponseDto(
                comment.getCommentId(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getAuthor().getUsername()
        );
    }
}