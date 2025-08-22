package com.tuniv.backend.qa.service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerCreateRequest;
import com.tuniv.backend.qa.dto.QuestionCreateRequest;
import com.tuniv.backend.qa.event.NewAnswerEvent;
import com.tuniv.backend.qa.model.*;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.AnswerVoteRepository;
import com.tuniv.backend.qa.repository.QuestionRepository;
import com.tuniv.backend.qa.repository.QuestionVoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final QuestionVoteRepository questionVoteRepository;
    private final AnswerVoteRepository answerVoteRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int QUESTION_UPVOTE_REP = 5;
    private static final int ANSWER_UPVOTE_REP = 10;
    private static final int DOWNVOTE_REP = -2;

    @Transactional
    public Question createQuestion(QuestionCreateRequest request, Integer moduleId, UserDetailsImpl currentUser) {
        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Module not found with id: " + moduleId));

        Question question = Question.builder()
                .title(request.title())
                .body(request.body())
                .module(module)
                .author(author)
                .build();

        return questionRepository.save(question);
    }

    @Transactional
    public Answer addAnswer(AnswerCreateRequest request, Integer questionId, UserDetailsImpl currentUser) {
        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Question not found with id: " + questionId));

        Answer answer = Answer.builder()
                .body(request.body())
                .question(question)
                .author(author)
                .isSolution(false)
                .build();

        Answer savedAnswer = answerRepository.save(answer);

        NewAnswerEvent event = new NewAnswerEvent(
            this,
            question.getTitle(),
            question.getAuthor().getEmail(),
            author.getUsername()
        );
        eventPublisher.publishEvent(event);

        return savedAnswer;
    }

    public List<Question> getQuestionsByModule(Integer moduleId) {
        return questionRepository.findByModuleModuleId(moduleId);
    }

    @Transactional
    public void voteOnQuestion(Integer questionId, UserDetailsImpl currentUser, int value) {
        User voter = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Voter not found"));
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));
        User author = question.getAuthor();

        if (voter.getUserId().equals(author.getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }

        // The constructor call will now work correctly
        QuestionVote.QuestionVoteId voteId = new QuestionVote.QuestionVoteId(voter.getUserId(), questionId);
        Optional<QuestionVote> existingVoteOpt = questionVoteRepository.findById(voteId);

        int reputationChange = 0;
        if (existingVoteOpt.isPresent()) {
            QuestionVote existingVote = existingVoteOpt.get();
            if (existingVote.getValue() == value) {
                reputationChange = (existingVote.getValue() == 1) ? -QUESTION_UPVOTE_REP : -DOWNVOTE_REP;
                questionVoteRepository.delete(existingVote);
            } else {
                reputationChange = calculateReputationChange(existingVote.getValue(), value, QUESTION_UPVOTE_REP);
                existingVote.setValue(value);
                questionVoteRepository.save(existingVote);
            }
        } else {
            reputationChange = (value == 1) ? QUESTION_UPVOTE_REP : DOWNVOTE_REP;
            QuestionVote newVote = QuestionVote.builder().id(voteId).user(voter).question(question).value(value).build();
            questionVoteRepository.save(newVote);
        }

        author.setReputationScore(author.getReputationScore() + reputationChange);
        userRepository.save(author);
    }

    @Transactional
    public void voteOnAnswer(Integer answerId, UserDetailsImpl currentUser, int value) {
        User voter = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Voter not found"));
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));
        User author = answer.getAuthor();

        if (voter.getUserId().equals(author.getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }

        AnswerVote.AnswerVoteId voteId = new AnswerVote.AnswerVoteId(voter.getUserId(), answerId);
        Optional<AnswerVote> existingVoteOpt = answerVoteRepository.findById(voteId);

        int reputationChange = 0;
        if (existingVoteOpt.isPresent()) {
            AnswerVote existingVote = existingVoteOpt.get();
            if (existingVote.getValue() == value) {
                reputationChange = (existingVote.getValue() == 1) ? -ANSWER_UPVOTE_REP : -DOWNVOTE_REP;
                answerVoteRepository.delete(existingVote);
            } else {
                reputationChange = calculateReputationChange(existingVote.getValue(), value, ANSWER_UPVOTE_REP);
                existingVote.setValue(value);
                answerVoteRepository.save(existingVote);
            }
        } else {
            reputationChange = (value == 1) ? ANSWER_UPVOTE_REP : DOWNVOTE_REP;
            AnswerVote newVote = AnswerVote.builder().id(voteId).user(voter).answer(answer).value(value).build();
            answerVoteRepository.save(newVote);
        }

        author.setReputationScore(author.getReputationScore() + reputationChange);
        userRepository.save(author);
    }

    private int calculateReputationChange(int oldValue, int newValue, int upvoteRep) {
        int newReputation = (newValue == 1) ? upvoteRep : DOWNVOTE_REP;
        int oldReputation = (oldValue == 1) ? upvoteRep : DOWNVOTE_REP;
        return newReputation - oldReputation;
    }
}