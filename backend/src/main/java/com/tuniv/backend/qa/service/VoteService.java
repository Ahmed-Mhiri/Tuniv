package com.tuniv.backend.qa.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.AnswerVote;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.CommentVote;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.model.QuestionVote;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.AnswerVoteRepository;
import com.tuniv.backend.qa.repository.CommentRepository;
import com.tuniv.backend.qa.repository.CommentVoteRepository;
import com.tuniv.backend.qa.repository.QuestionRepository;
import com.tuniv.backend.qa.repository.QuestionVoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CommentRepository commentRepository;
    private final QuestionVoteRepository questionVoteRepository;
    private final AnswerVoteRepository answerVoteRepository;
    private final CommentVoteRepository commentVoteRepository;

    private static final int QUESTION_UPVOTE_REP = 5;
    private static final int ANSWER_UPVOTE_REP = 10;
    private static final int COMMENT_UPVOTE_REP = 2;
    private static final int DOWNVOTE_REP = -2;

    @Transactional
    public void voteOnQuestion(Integer questionId, UserDetailsImpl currentUser, int value) {
        User voter = userRepository.findById(currentUser.getId()).orElseThrow(() -> new ResourceNotFoundException("Voter not found"));
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));
        User author = question.getAuthor();

        if (voter.getUserId().equals(author.getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }

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
            QuestionVote newVote = new QuestionVote(voteId, voter, question, value);
            questionVoteRepository.save(newVote);
            reputationChange = (value == 1) ? QUESTION_UPVOTE_REP : DOWNVOTE_REP;
        }

        author.setReputationScore(author.getReputationScore() + reputationChange);
        userRepository.save(author);
    }

    @Transactional
    public void voteOnAnswer(Integer answerId, UserDetailsImpl currentUser, int value) {
        User voter = userRepository.findById(currentUser.getId()).orElseThrow(() -> new ResourceNotFoundException("Voter not found"));
        Answer answer = answerRepository.findById(answerId).orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));
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
            AnswerVote newVote = new AnswerVote(voteId, voter, answer, value);
            answerVoteRepository.save(newVote);
            reputationChange = (value == 1) ? ANSWER_UPVOTE_REP : DOWNVOTE_REP;
        }

        author.setReputationScore(author.getReputationScore() + reputationChange);
        userRepository.save(author);
    }

    @Transactional
    public void voteOnComment(Integer commentId, UserDetailsImpl currentUser, int value) {
        User voter = userRepository.findById(currentUser.getId()).orElseThrow(() -> new ResourceNotFoundException("Voter not found"));
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        User author = comment.getAuthor();

        if (voter.getUserId().equals(author.getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }

        CommentVote.CommentVoteId voteId = new CommentVote.CommentVoteId(voter.getUserId(), commentId);
        Optional<CommentVote> existingVoteOpt = commentVoteRepository.findById(voteId);
        
        int reputationChange = 0;
        if (existingVoteOpt.isPresent()) {
            CommentVote existingVote = existingVoteOpt.get();
            if (existingVote.getValue() == value) {
                reputationChange = (existingVote.getValue() == 1) ? -COMMENT_UPVOTE_REP : -DOWNVOTE_REP;
                commentVoteRepository.delete(existingVote);
            } else {
                reputationChange = calculateReputationChange(existingVote.getValue(), value, COMMENT_UPVOTE_REP);
                existingVote.setValue(value);
                commentVoteRepository.save(existingVote);
            }
        } else {
            CommentVote newVote = new CommentVote(voteId, voter, comment, value);
            commentVoteRepository.save(newVote);
            reputationChange = (value == 1) ? COMMENT_UPVOTE_REP : DOWNVOTE_REP;
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