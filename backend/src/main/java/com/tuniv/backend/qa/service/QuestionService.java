package com.tuniv.backend.qa.service;

// imports...
import com.tuniv.backend.qa.model.*;
import com.tuniv.backend.qa.repository.AnswerVoteRepository;
import com.tuniv.backend.qa.repository.QuestionVoteRepository;
import org.springframework.transaction.annotation.Transactional;

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

    // Reputation point constants
    private static final int QUESTION_UPVOTE_REP = 5;
    private static final int ANSWER_UPVOTE_REP = 10;
    private static final int DOWNVOTE_REP = -2;

    // ... (createQuestion, addAnswer, and getQuestionsByModule methods are the same as before) ...

    @Transactional
    public void voteOnQuestion(Integer questionId, UserDetailsImpl currentUser, int value) {
        User voter = userRepository.findById(currentUser.getId()).orElseThrow(() -> new RuntimeException("Voter not found"));
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new RuntimeException("Question not found"));
        User author = question.getAuthor();

        if (voter.getUserId().equals(author.getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }

        QuestionVote.QuestionVoteId voteId = new QuestionVote.QuestionVoteId(voter.getUserId(), questionId);
        Optional<QuestionVote> existingVoteOpt = questionVoteRepository.findById(voteId);

        int reputationChange = 0;
        if (existingVoteOpt.isPresent()) {
            QuestionVote existingVote = existingVoteOpt.get();
            // User is changing their vote (e.g., from upvote to downvote)
            if (existingVote.getValue() != value) {
                reputationChange = (value == 1) ? QUESTION_UPVOTE_REP - DOWNVOTE_REP : DOWNVOTE_REP - QUESTION_UPVOTE_REP;
                existingVote.setValue(value);
                questionVoteRepository.save(existingVote);
            }
        } else {
            // New vote
            reputationChange = (value == 1) ? QUESTION_UPVOTE_REP : DOWNVOTE_REP;
            QuestionVote newVote = QuestionVote.builder().id(voteId).user(voter).question(question).value(value).build();
            questionVoteRepository.save(newVote);
        }

        author.setReputationScore(author.getReputationScore() + reputationChange);
        userRepository.save(author);
    }

    @Transactional
    public void voteOnAnswer(Integer answerId, UserDetailsImpl currentUser, int value) {
        User voter = userRepository.findById(currentUser.getId()).orElseThrow(() -> new RuntimeException("Voter not found"));
        Answer answer = answerRepository.findById(answerId).orElseThrow(() -> new RuntimeException("Answer not found"));
        User author = answer.getAuthor();

        if (voter.getUserId().equals(author.getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }

        AnswerVote.AnswerVoteId voteId = new AnswerVote.AnswerVoteId(voter.getUserId(), answerId);
        Optional<AnswerVote> existingVoteOpt = answerVoteRepository.findById(voteId);

        int reputationChange = 0;
        if (existingVoteOpt.isPresent()) {
            AnswerVote existingVote = existingVoteOpt.get();
            if (existingVote.getValue() != value) {
                reputationChange = (value == 1) ? ANSWER_UPVOTE_REP - DOWNVOTE_REP : DOWNVOTE_REP - ANSWER_UPVOTE_REP;
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
}