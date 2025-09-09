package com.tuniv.backend.qa.service;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewVoteEvent;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.AnswerVote;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.CommentVote;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.model.QuestionVote;
import com.tuniv.backend.qa.model.Vote;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.CommentRepository;
import com.tuniv.backend.qa.repository.QuestionRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
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
    private final VoteRepository voteRepository; // ✨ INJECT THE CONSOLIDATED REPOSITORY
    private final ApplicationEventPublisher eventPublisher;

    private static final int QUESTION_UPVOTE_REP = 5;
    private static final int ANSWER_UPVOTE_REP = 10;
    private static final int COMMENT_UPVOTE_REP = 2;
    private static final int DOWNVOTE_REP = -2;

    @Transactional
    public void voteOnQuestion(Integer questionId, UserDetailsImpl currentUser, int value) {
        User voter = findVoter(currentUser);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (voter.getUserId().equals(question.getAuthor().getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }
        
        // ✅ NEW LOGIC: Find existing vote by user and post
        Optional<Vote> existingVote = voteRepository.findByUser_IdAndPost_Id(voter.getUserId(), questionId);
        // ✅ NEW LOGIC: Create vote object with the simple constructor
        QuestionVote newVote = new QuestionVote(voter, question, (short) value);

        processVote(voter, question, question.getAuthor(), existingVote, newVote, value, QUESTION_UPVOTE_REP, questionRepository);
    }

    @Transactional
    public void voteOnAnswer(Integer answerId, UserDetailsImpl currentUser, int value) {
        User voter = findVoter(currentUser);
        Answer answer = answerRepository.findWithQuestionById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found"));

        if (voter.getUserId().equals(answer.getAuthor().getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }

        // ✅ NEW LOGIC: Find existing vote by user and post
        Optional<Vote> existingVote = voteRepository.findByUser_IdAndPost_Id(voter.getUserId(), answerId);
        // ✅ NEW LOGIC: Create vote object with the simple constructor
        AnswerVote newVote = new AnswerVote(voter, answer, (short) value);

        processVote(voter, answer, answer.getAuthor(), existingVote, newVote, value, ANSWER_UPVOTE_REP, answerRepository);
    }

    @Transactional
    public void voteOnComment(Integer commentId, UserDetailsImpl currentUser, int value) {
        User voter = findVoter(currentUser);
        Comment comment = commentRepository.findWithParentsById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (voter.getUserId().equals(comment.getAuthor().getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }

        // ✅ NEW LOGIC: Find existing vote by user and post
        Optional<Vote> existingVote = voteRepository.findByUser_IdAndPost_Id(voter.getUserId(), commentId);
        // ✅ NEW LOGIC: Create vote object with the simple constructor
        CommentVote newVote = new CommentVote(voter, comment, (short) value);

        processVote(voter, comment, comment.getAuthor(), existingVote, newVote, value, COMMENT_UPVOTE_REP, commentRepository);
    }

    private <P extends Post> void processVote(
            User voter, P post, User author, Optional<Vote> existingVoteOpt, Vote newVote, int value,
            int upvoteReputation, JpaRepository<P, Integer> postRepository
    ) {
        int reputationChange = 0;
        short shortValue = (short) value;

        if (existingVoteOpt.isPresent()) {
            Vote existingVote = existingVoteOpt.get();
            if (existingVote.getValue() == shortValue) { // Undoing vote
                reputationChange = (shortValue == 1) ? -upvoteReputation : -DOWNVOTE_REP;
                voteRepository.delete(existingVote);
                post.setScore(post.getScore() - shortValue);
            } else { // Changing vote
                reputationChange = calculateReputationChange(existingVote.getValue(), shortValue, upvoteReputation);
                post.setScore(post.getScore() - existingVote.getValue() + shortValue);
                existingVote.setValue(shortValue);
                voteRepository.save(existingVote);
            }
        } else { // New vote
            reputationChange = (shortValue == 1) ? upvoteReputation : DOWNVOTE_REP;
            post.setScore(post.getScore() + shortValue);
            voteRepository.save(newVote);
            
            if (shortValue == 1) { // Only notify on upvotes
                // ✅ NEW LOGIC: Use the Post object from the vote for context
                Post votedPost = newVote.getPost();
                if (votedPost instanceof Question) {
                    eventPublisher.publishEvent(new NewVoteEvent(this, voter.getUserId(), author.getUserId(), Post.PostType.QUESTION, votedPost.getId(), ((Question) votedPost).getTitle(), votedPost.getId()));
                } else if (votedPost instanceof Answer) {
                    eventPublisher.publishEvent(new NewVoteEvent(this, voter.getUserId(), author.getUserId(), Post.PostType.ANSWER, votedPost.getId(), ((Answer) votedPost).getQuestion().getTitle(), ((Answer) votedPost).getQuestion().getId()));
                } else if (votedPost instanceof Comment) {
                     eventPublisher.publishEvent(new NewVoteEvent(this, voter.getUserId(), author.getUserId(), Post.PostType.COMMENT, votedPost.getId(), ((Comment) votedPost).getAnswer().getQuestion().getTitle(), ((Comment) votedPost).getAnswer().getQuestion().getId()));
                }
            }
        }
        
        author.setReputationScore(author.getReputationScore() + reputationChange);
        userRepository.save(author);
        postRepository.save(post);
    }

    private User findVoter(UserDetailsImpl currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Voter not found"));
    }

    private int calculateReputationChange(int oldValue, int newValue, int upvoteRep) {
        int oldReputation = (oldValue == 1) ? upvoteRep : DOWNVOTE_REP;
        int newReputation = (newValue == 1) ? upvoteRep : DOWNVOTE_REP;
        return newReputation - oldReputation;
    }
}
