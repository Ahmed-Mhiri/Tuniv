package com.tuniv.backend.qa.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.auth.service.PostAuthorizationService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerResponseDto;
import com.tuniv.backend.qa.dto.AnswerUpdateRequest;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PostAuthorizationService postAuthorizationService;
    private final AttachmentService attachmentService;

    private static final int ACCEPT_ANSWER_REP = 15; // For the answer author
    private static final int ACCEPTS_ANSWER_REP = 2;  // For the question author

    @Transactional
    @CacheEvict(value = "questions", key = "#result.question.id") // Use .id as per Post entity
    public Answer markAsSolution(Integer answerId, UserDetailsImpl currentUserDetails) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        Question question = answer.getQuestion();
        User questionAuthor = question.getAuthor();
        User answerAuthor = answer.getAuthor();

        // Security Check: Only the person who asked the question can mark a solution.
        if (!questionAuthor.getUserId().equals(currentUserDetails.getId())) {
            throw new AccessDeniedException("Only the author of the question can mark an answer as the solution.");
        }

        // Prevent marking own answer as solution
        if (questionAuthor.getUserId().equals(answerAuthor.getUserId())) {
            throw new IllegalArgumentException("You cannot mark your own answer as the solution.");
        }

        // Set the solution flag and save
        answer.setIsSolution(true);
        answerRepository.save(answer);

        // Award reputation points
        answerAuthor.setReputationScore(answerAuthor.getReputationScore() + ACCEPT_ANSWER_REP);
        questionAuthor.setReputationScore(questionAuthor.getReputationScore() + ACCEPTS_ANSWER_REP);

        userRepository.save(answerAuthor);
        userRepository.save(questionAuthor);
        // eventPublisher.publishEvent(new SolutionMarkedEvent(this, answer)); // Uncomment if you have this event

        return answer;
    }

    @Transactional
    @CacheEvict(value = "questions", allEntries = true)
    public AnswerResponseDto updateAnswer(Integer answerId, AnswerUpdateRequest request, List<MultipartFile> newFiles, UserDetailsImpl currentUser) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        postAuthorizationService.checkOwnership(answer, currentUser);

        // Update body
        answer.setBody(request.body());
        
        // ✨ --- REFACTORED DELETION LOGIC --- ✨
        if (request.attachmentIdsToDelete() != null && !request.attachmentIdsToDelete().isEmpty()) {
            Set<Attachment> toDelete = answer.getAttachments().stream()
                    .filter(att -> request.attachmentIdsToDelete().contains(att.getAttachmentId()))
                    .collect(Collectors.toSet());

            // Delete the physical files first
            attachmentService.deleteAttachments(toDelete);
            
            // Use the helper method to ensure both sides of the relationship are updated in memory
            toDelete.forEach(answer::removeAttachment);
        }

        // Add new attachments using the helper method inside this service
        attachmentService.saveAttachments(newFiles, answer);

        Answer updatedAnswer = answerRepository.save(answer);
        
        // Note: For a complete DTO, you'd need to fetch votes here like in your get methods.
        return QAMapper.toAnswerResponseDto(updatedAnswer, currentUser, Collections.emptyMap(), Collections.emptyMap());
    }

    @Transactional
    @CacheEvict(value = "questions", allEntries = true)
    public void deleteAnswer(Integer answerId, UserDetailsImpl currentUser) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        postAuthorizationService.checkOwnership(answer, currentUser);

        // We must trigger the physical file cleanup before the entity is deleted.
        // JPA's orphanRemoval will handle deleting the database records.
        attachmentService.deleteAttachments(answer.getAttachments());
        
        answerRepository.delete(answer);
    }
}