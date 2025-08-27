package com.tuniv.backend.qa.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.model.Answer;
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

    private static final int ACCEPT_ANSWER_REP = 15; // For the answer author
    private static final int ACCEPTS_ANSWER_REP = 2;  // For the question author

    @Transactional
    public void markAsSolution(Integer answerId, UserDetailsImpl currentUserDetails) {
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
    }
}
