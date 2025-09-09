package com.tuniv.backend.qa.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.auth.service.PostAuthorizationService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerUpdateRequest;
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
    private final PostAuthorizationService postAuthorizationService;
    private final AttachmentService attachmentService;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager; // ✅ Injected for manual eviction

    private static final int ACCEPT_ANSWER_REP = 15; // For the answer author
    private static final int ACCEPTS_ANSWER_REP = 2;  // For the question author

    @Transactional
    @CacheEvict(value = "questions", key = "#result.question.id")
    public Answer markAsSolution(Integer answerId, UserDetailsImpl currentUserDetails) {
        Answer answer = answerRepository.findWithDetailsById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        Question question = answer.getQuestion();
        User questionAuthor = question.getAuthor();
        User answerAuthor = answer.getAuthor();

        if (!questionAuthor.getUserId().equals(currentUserDetails.getId())) {
            throw new AccessDeniedException("Only the author of the question can mark an answer as the solution.");
        }

        if (questionAuthor.getUserId().equals(answerAuthor.getUserId())) {
            throw new IllegalArgumentException("You cannot mark your own answer as the solution.");
        }
        
        // Unmark any previous solution
        answerRepository.findSolutionByQuestionId(question.getId()).ifPresent(currentSolution -> {
            currentSolution.setIsSolution(false);
            answerRepository.save(currentSolution);
        });

        answer.setIsSolution(true);
        answerRepository.save(answer);

        answerAuthor.setReputationScore(answerAuthor.getReputationScore() + ACCEPT_ANSWER_REP);
        questionAuthor.setReputationScore(questionAuthor.getReputationScore() + ACCEPTS_ANSWER_REP);

        userRepository.save(answerAuthor);
        userRepository.save(questionAuthor);

        return answer;
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#result.question.id")
    public Answer updateAnswer(Integer answerId, AnswerUpdateRequest request, List<MultipartFile> newFiles, UserDetailsImpl currentUser) {
        Answer answer = answerRepository.findWithQuestionById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        postAuthorizationService.checkOwnership(answer, currentUser);

        answer.setBody(request.body());
        
        if (request.attachmentIdsToDelete() != null && !request.attachmentIdsToDelete().isEmpty()) {
            Set<Attachment> toDelete = answer.getAttachments().stream()
                    .filter(att -> request.attachmentIdsToDelete().contains(att.getAttachmentId()))
                    .collect(Collectors.toSet());

            attachmentService.deleteAttachments(toDelete);
            toDelete.forEach(answer::removeAttachment);
        }

        attachmentService.saveAttachments(newFiles, answer);
        return answerRepository.save(answer);
    }

    @Transactional
    // ❌ The invalid @CacheEvict annotation has been removed.
    public void deleteAnswer(Integer answerId, UserDetailsImpl currentUser) {
        // Fetch the answer with its question so we have the ID for cache eviction.
        Answer answer = answerRepository.findWithQuestionById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        postAuthorizationService.checkOwnership(answer, currentUser);

        // ✅ THE FIX: Manually find the question ID and evict the cache entry.
        Integer questionId = answer.getQuestion().getId();
        Cache questionsCache = cacheManager.getCache("questions");
        if (questionsCache != null) {
            questionsCache.evict(questionId);
        }
        
        // Now, proceed with the deletion.
        attachmentService.deleteAttachments(answer.getAttachments());
        answerRepository.delete(answer);
    }
}