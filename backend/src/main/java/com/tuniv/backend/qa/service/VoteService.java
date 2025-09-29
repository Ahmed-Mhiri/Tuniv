package com.tuniv.backend.qa.service;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewVoteEvent;

import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.qa.model.PostType;
import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.ReplyVote;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.qa.model.TopicVote;

import com.tuniv.backend.qa.model.VotablePost;
import com.tuniv.backend.qa.model.Vote;
import com.tuniv.backend.qa.repository.ReplyRepository;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final ReplyRepository replyRepository;
    private final VoteRepository voteRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int TOPIC_UPVOTE_REP = 5;
    private static final int REPLY_UPVOTE_REP = 10;
    private static final int DOWNVOTE_REP = -2;

    @Transactional
    public void voteOnTopic(Integer topicId, UserDetailsImpl currentUser, int value) {
        User voter = findVoter(currentUser);
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        if (voter.getUserId().equals(topic.getAuthor().getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }
        
        Optional<Vote> existingVote = voteRepository.findByUser_UserIdAndPost_Id(voter.getUserId(), topicId);
        TopicVote newVote = new TopicVote(voter, topic, (short) value);

        processVote(voter, topic, topic.getAuthor(), existingVote, newVote, value, TOPIC_UPVOTE_REP, topicRepository);
    }

    @Transactional
    public void voteOnReply(Integer replyId, UserDetailsImpl currentUser, int value) {
        User voter = findVoter(currentUser);
        Reply reply = replyRepository.findWithTopicById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found"));

        if (voter.getUserId().equals(reply.getAuthor().getUserId())) {
            throw new IllegalArgumentException("You cannot vote on your own post.");
        }

        Optional<Vote> existingVote = voteRepository.findByUser_UserIdAndPost_Id(voter.getUserId(), replyId);
        ReplyVote newVote = new ReplyVote(voter, reply, (short) value);

        processVote(voter, reply, reply.getAuthor(), existingVote, newVote, value, REPLY_UPVOTE_REP, replyRepository);
    }

    private <P extends VotablePost> void processVote(
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
                Post votedPost = newVote.getPost();
                if (votedPost instanceof Topic topic) {
                    // ✅ Using convenience constructor for Topic vote
                    eventPublisher.publishEvent(new NewVoteEvent(
                        voter.getUserId(), // voterId
                        author.getUserId(), // authorId
                        PostType.TOPIC, // postType
                        votedPost.getId(), // postId (topic ID)
                        topic.getTitle(), // questionTitle
                        votedPost.getId() // questionId (same as topic ID for topics)
                    ));
                } else if (votedPost instanceof Reply reply) {
                    // ✅ Using convenience constructor for Reply vote
                    eventPublisher.publishEvent(new NewVoteEvent(
                        voter.getUserId(), // voterId
                        author.getUserId(), // authorId
                        PostType.REPLY, // postType
                        votedPost.getId(), // postId (reply ID)
                        reply.getTopic().getTitle(), // questionTitle (from parent topic)
                        reply.getTopic().getId() // questionId (parent topic ID)
                    ));
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