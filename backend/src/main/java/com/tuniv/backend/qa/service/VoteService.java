package com.tuniv.backend.qa.service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewVoteEvent;
import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.qa.model.VotablePost;
import com.tuniv.backend.qa.model.Vote;
import com.tuniv.backend.qa.repository.ReplyRepository;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final ReplyRepository replyRepository;
    private final VoteRepository voteRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Reputation constants
    private static final int TOPIC_UPVOTE_REP = 5;
    private static final int REPLY_UPVOTE_REP = 10;
    private static final int DOWNVOTE_REP = -2;

    @Transactional
    public void voteOnTopic(Integer topicId, UserDetailsImpl currentUser, int value) {
        User voter = findVoter(currentUser);
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));
        
        processVote(voter, topic, value, TOPIC_UPVOTE_REP);
    }

    @Transactional
    public void voteOnReply(Integer replyId, UserDetailsImpl currentUser, int value) {
        User voter = findVoter(currentUser);
        Reply reply = replyRepository.findById(replyId)
            .orElseThrow(() -> new ResourceNotFoundException("Reply not found with id: " + replyId));
        
        processVote(voter, reply, value, REPLY_UPVOTE_REP);
    }

    /**
     * Generic, central method for processing a vote on any VotablePost.
     * It handles new votes, undoing votes, and changing votes, while updating
     * all scores and denormalized counters atomically.
     */
    private <P extends VotablePost> void processVote(User voter, P post, int value, int upvoteReputationValue) {
        if (voter.getUserId().equals(post.getAuthor().getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }

        User author = post.getAuthor();
        short shortValue = (short) value;
        Optional<Vote> existingVoteOpt = voteRepository.findByUser_UserIdAndPost_Id(voter.getUserId(), post.getId());

        if (existingVoteOpt.isPresent()) {
            // Existing vote found: user is either undoing or changing their vote.
            Vote existingVote = existingVoteOpt.get();
            short oldValue = existingVote.getValue();

            if (oldValue == shortValue) { // --- UNDOING VOTE ---
                log.debug("User {} undoing vote on post {}", voter.getUsername(), post.getId());
                voteRepository.delete(existingVote);
                updateCountsForUndo(post, author, oldValue, upvoteReputationValue);
            } else { // --- CHANGING VOTE ---
                log.debug("User {} changing vote on post {} from {} to {}", voter.getUsername(), post.getId(), oldValue, shortValue);
                existingVote.setValue(shortValue);
                voteRepository.save(existingVote);
                updateCountsForChange(post, author, oldValue, shortValue, upvoteReputationValue);
            }
        } else { // --- NEW VOTE ---
            log.debug("User {} casting new vote on post {}", voter.getUsername(), post.getId());
            Vote newVote = new Vote(voter, post, shortValue);
            voteRepository.save(newVote);
            updateCountsForNew(post, author, shortValue, upvoteReputationValue);
            
            if (shortValue == 1) { // Only publish event for new upvotes
                // eventPublisher.publishEvent(new NewVoteEvent(this, newVote));
            }
        }
        
        // Save the updated entities
        userRepository.save(author);
        // The specific repository save is needed if the generic type is not automatically flushed
        if (post instanceof Topic) topicRepository.save((Topic) post);
        if (post instanceof Reply) replyRepository.save((Reply) post);
    }

    //<editor-fold desc="Count Update Helpers">
    private void updateCountsForNew(VotablePost post, User author, short value, int upvoteRep) {
        post.setScore(post.getScore() + value);
        if (value == 1) {
            post.setUpvoteCount(post.getUpvoteCount() + 1);
            author.setReputationScore(author.getReputationScore() + upvoteRep);
            author.setHelpfulVotesReceivedCount(author.getHelpfulVotesReceivedCount() + 1);
        } else { // value == -1
            post.setDownvoteCount(post.getDownvoteCount() + 1);
            author.setReputationScore(author.getReputationScore() + DOWNVOTE_REP);
        }
    }

    private void updateCountsForUndo(VotablePost post, User author, short oldValue, int upvoteRep) {
        post.setScore(post.getScore() - oldValue);
        if (oldValue == 1) {
            post.setUpvoteCount(post.getUpvoteCount() - 1);
            author.setReputationScore(author.getReputationScore() - upvoteRep);
            author.setHelpfulVotesReceivedCount(author.getHelpfulVotesReceivedCount() - 1);
        } else { // oldValue == -1
            post.setDownvoteCount(post.getDownvoteCount() - 1);
            author.setReputationScore(author.getReputationScore() - DOWNVOTE_REP);
        }
    }

    private void updateCountsForChange(VotablePost post, User author, short oldValue, short newValue, int upvoteRep) {
        // Net score change is always 2 or -2
        post.setScore(post.getScore() + (newValue - oldValue));
        
        if (oldValue == 1 && newValue == -1) { // From Upvote to Downvote
            post.setUpvoteCount(post.getUpvoteCount() - 1);
            post.setDownvoteCount(post.getDownvoteCount() + 1);
            author.setReputationScore(author.getReputationScore() - upvoteRep + DOWNVOTE_REP);
            author.setHelpfulVotesReceivedCount(author.getHelpfulVotesReceivedCount() - 1);
        } else { // From Downvote to Upvote (oldValue == -1 && newValue == 1)
            post.setDownvoteCount(post.getDownvoteCount() - 1);
            post.setUpvoteCount(post.getUpvoteCount() + 1);
            author.setReputationScore(author.getReputationScore() - DOWNVOTE_REP + upvoteRep);
            author.setHelpfulVotesReceivedCount(author.getHelpfulVotesReceivedCount() + 1);
        }
    }
    //</editor-fold>

    private User findVoter(UserDetailsImpl currentUser) {
        return userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Voter not found"));
    }
}