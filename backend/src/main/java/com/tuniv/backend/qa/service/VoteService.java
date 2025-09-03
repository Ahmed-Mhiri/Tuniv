package com.tuniv.backend.qa.service;

import java.io.Serializable;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewVoteEvent;
import com.tuniv.backend.notification.model.PostType;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.AnswerVote;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.CommentVote;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.model.QuestionVote;
import com.tuniv.backend.qa.model.Vote;
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
    private final ApplicationEventPublisher eventPublisher;

    private static final int QUESTION_UPVOTE_REP = 5;
    private static final int ANSWER_UPVOTE_REP = 10;
    private static final int COMMENT_UPVOTE_REP = 2;
    private static final int DOWNVOTE_REP = -2;

    @Transactional
    @CacheEvict(value = "questions", key = "#questionId")
    public void voteOnQuestion(Integer questionId, UserDetailsImpl currentUser, short value) {
        User voter = findVoter(currentUser);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        
        if (voter.getUserId().equals(question.getAuthor().getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }
        
        QuestionVote.QuestionVoteId voteId = new QuestionVote.QuestionVoteId(voter.getUserId(), questionId);
        Optional<QuestionVote> existingVote = questionVoteRepository.findById(voteId);
        
        QuestionVote newVote = QuestionVote.builder().id(voteId).user(voter).question(question).value(value).build();

        processVote(question.getAuthor(), existingVote, newVote, value, QUESTION_UPVOTE_REP, questionVoteRepository);
    }

    @Transactional
    @CacheEvict(value = "questions", allEntries = true)
    public void voteOnAnswer(Integer answerId, UserDetailsImpl currentUser, short value) {
        User voter = findVoter(currentUser);
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found"));

        if (voter.getUserId().equals(answer.getAuthor().getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }
        
        AnswerVote.AnswerVoteId voteId = new AnswerVote.AnswerVoteId(voter.getUserId(), answerId);
        Optional<AnswerVote> existingVote = answerVoteRepository.findById(voteId);

        AnswerVote newVote = AnswerVote.builder().id(voteId).user(voter).answer(answer).value(value).build();
        
        processVote(answer.getAuthor(), existingVote, newVote, value, ANSWER_UPVOTE_REP, answerVoteRepository);
    }

    @Transactional
    @CacheEvict(value = "questions", allEntries = true)
    public void voteOnComment(Integer commentId, UserDetailsImpl currentUser, short value) {
        User voter = findVoter(currentUser);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (voter.getUserId().equals(comment.getAuthor().getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }

        CommentVote.CommentVoteId voteId = new CommentVote.CommentVoteId(voter.getUserId(), commentId);
        Optional<CommentVote> existingVote = commentVoteRepository.findById(voteId);

        // --- THE FIX: Use the builder pattern for consistency and to avoid constructor errors. ---
        CommentVote newVote = CommentVote.builder()
                .id(voteId)
                .user(voter)
                .comment(comment)
                .value(value)
                .build();

        processVote(comment.getAuthor(), existingVote, newVote, value, COMMENT_UPVOTE_REP, commentVoteRepository);
    }

    private <V extends Vote, ID extends Serializable> void processVote(
            User author,
            Optional<V> existingVoteOpt,
            V newVote,
            short value,
            int upvoteReputation,
            JpaRepository<V, ID> voteRepository
    ) {
        int reputationChange = 0;

        if (existingVoteOpt.isPresent()) {
            V existingVote = existingVoteOpt.get();
            if (existingVote.getValue() == value) {
                // User is retracting their vote
                reputationChange = (existingVote.getValue() == 1) ? -upvoteReputation : -DOWNVOTE_REP;
                voteRepository.delete(existingVote);
            } else {
                // User is changing their vote (e.g., from up to down)
                reputationChange = calculateReputationChange(existingVote.getValue(), value, upvoteReputation);
                if (existingVote instanceof QuestionVote) ((QuestionVote) existingVote).setValue(value);
                if (existingVote instanceof AnswerVote) ((AnswerVote) existingVote).setValue(value);
                if (existingVote instanceof CommentVote) ((CommentVote) existingVote).setValue(value);
                voteRepository.save(existingVote);
            }
        } else {
            // This is a new vote
            voteRepository.save(newVote);
            reputationChange = (value == 1) ? upvoteReputation : DOWNVOTE_REP;
            
            if (value == 1) { // Publish event only for new upvotes
                User voter = (User) newVote.getUser();
                
                if (newVote instanceof QuestionVote qv) {
                    eventPublisher.publishEvent(new NewVoteEvent(this, voter.getUserId(), author.getUserId(),
                        PostType.QUESTION, qv.getPostId(), qv.getQuestion().getTitle(), qv.getPostId()));
                
                } else if (newVote instanceof AnswerVote av) {
                    eventPublisher.publishEvent(new NewVoteEvent(this, voter.getUserId(), author.getUserId(),
                        PostType.ANSWER, av.getPostId(), av.getAnswer().getQuestion().getTitle(), av.getAnswer().getQuestion().getId()));
                
                } else if (newVote instanceof CommentVote cv) {
                    eventPublisher.publishEvent(new NewVoteEvent(this, voter.getUserId(), author.getUserId(),
                        PostType.COMMENT, cv.getPostId(), cv.getComment().getAnswer().getQuestion().getTitle(), cv.getComment().getAnswer().getQuestion().getId()));
                }
            }
        }

        author.setReputationScore(author.getReputationScore() + reputationChange);
        userRepository.save(author);
    }

    private User findVoter(UserDetailsImpl currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Voter not found"));
    }

    private int calculateReputationChange(int oldValue, int newValue, int upvoteRep) {
        int newReputation = (newValue == 1) ? upvoteRep : DOWNVOTE_REP;
        int oldReputation = (oldValue == 1) ? upvoteRep : DOWNVOTE_REP;
        return newReputation - oldReputation;
    }
}