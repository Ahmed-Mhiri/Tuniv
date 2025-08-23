package com.tuniv.backend.qa.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service; // <-- IMPORT ADDED
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.CommentCreateRequest;
import com.tuniv.backend.qa.dto.CommentResponseDto;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.repository.AnswerRepository;
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

        attachmentService.saveAttachments(files, savedComment.getCommentId(), "COMMENT");

        // Use the central mapper
        return QAMapper.toCommentResponseDto(savedComment);
    }

    public List<CommentResponseDto> getCommentsByAnswer(Integer answerId) {
        if (!answerRepository.existsById(answerId)) {
            throw new ResourceNotFoundException("Answer not found with id: " + answerId);
        }
        return commentRepository.findByAnswerAnswerIdOrderByCreatedAtAsc(answerId)
                .stream()
                .map(QAMapper::toCommentResponseDto) // Use the central mapper
                .collect(Collectors.toList());
    }
    
    // The private mapToDto method has been removed.
}