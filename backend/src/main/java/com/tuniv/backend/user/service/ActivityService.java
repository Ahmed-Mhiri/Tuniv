package com.tuniv.backend.user.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.qa.model.TopicType;

import com.tuniv.backend.qa.repository.ReplyRepository;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.user.dto.UserActivityItemDto;
import com.tuniv.backend.user.dto.UserActivityItemDto.ActivityType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final TopicRepository topicRepository;
    private final ReplyRepository replyRepository;
    private final VoteRepository voteRepository;

    @Transactional(readOnly = true)
    public List<UserActivityItemDto> getActivityForUser(Integer userId) {
        // Fetch user activities
        var topics = topicRepository.findByAuthor_UserIdOrderByCreatedAtDesc(userId);
        var allReplies = replyRepository.findByAuthor_UserIdOrderByCreatedAtDesc(userId);
        var acceptedSolutions = getAcceptedSolutionsByUser(userId);
        var votes = voteRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);

        // Separate answers from comments
        var answers = allReplies.stream()
                .filter(this::isAnswer)
                .collect(Collectors.toList());
        var comments = allReplies.stream()
                .filter(this::isComment)
                .collect(Collectors.toList());

        // Convert to activity streams - FIXED CONSTRUCTOR CALLS
        Stream<UserActivityItemDto> topicActivities = topics.stream()
                .map(t -> new UserActivityItemDto(
                    UserActivityItemDto.ActivityType.TOPIC_CREATED,
                    t.getCreatedAt(), 
                    t.getScore(), 
                    null, // voteValue
                    t.getId(), // questionId (topic ID)
                    t.getTitle(), // questionTitle (topic title)
                    null, // answerId
                    t.isSolved(), // isSolution
                    null  // commentId
                ));

        Stream<UserActivityItemDto> answerActivities = answers.stream()
                .map(a -> new UserActivityItemDto(
                    UserActivityItemDto.ActivityType.ANSWER_POSTED,
                    a.getCreatedAt(), 
                    a.getScore(), 
                    null, // voteValue
                    a.getTopic().getId(), // questionId
                    a.getTopic().getTitle(), // questionTitle
                    a.getId(), // answerId
                    isReplyAccepted(a), // isSolution
                    null  // commentId
                ));

        Stream<UserActivityItemDto> commentActivities = comments.stream()
                .map(c -> new UserActivityItemDto(
                    UserActivityItemDto.ActivityType.COMMENT_POSTED,
                    c.getCreatedAt(), 
                    c.getScore(), 
                    null, // voteValue
                    c.getTopic().getId(), // questionId
                    c.getTopic().getTitle(), // questionTitle
                    null, // answerId
                    false, // isSolution (comments can't be solutions)
                    c.getId()  // commentId
                ));

        Stream<UserActivityItemDto> acceptedSolutionActivities = acceptedSolutions.stream()
                .map(r -> new UserActivityItemDto(
                    UserActivityItemDto.ActivityType.SOLUTION_ACCEPTED,
                    r.getTopic().getCreatedAt(), // or r.getCreatedAt()?
                    r.getScore(), 
                    null, // voteValue
                    r.getTopic().getId(), // questionId
                    r.getTopic().getTitle(), // questionTitle
                    r.getId(), // answerId
                    true, // isSolution
                    null  // commentId
                ));

        // Map votes - FIXED CONSTRUCTOR CALLS
        Stream<UserActivityItemDto> voteActivities = votes.stream().map(vote -> {
            Post post = vote.getPost();
            
            if (post instanceof Topic topic) {
                return new UserActivityItemDto(
                    UserActivityItemDto.ActivityType.VOTE_CAST, 
                    vote.getCreatedAt(), 
                    topic.getScore(), 
                    (int) vote.getValue(), // voteValue
                    topic.getId(), // questionId
                    topic.getTitle(), // questionTitle
                    null, // answerId
                    topic.isSolved(), // isSolution
                    null  // commentId
                );
            } else if (post instanceof Reply reply) {
                // Determine if it's an answer or comment vote
                if (isAnswer(reply)) {
                    return new UserActivityItemDto(
                        UserActivityItemDto.ActivityType.VOTE_CAST, 
                        vote.getCreatedAt(), 
                        reply.getScore(), 
                        (int) vote.getValue(), // voteValue
                        reply.getTopic().getId(), // questionId
                        reply.getTopic().getTitle(), // questionTitle
                        reply.getId(), // answerId
                        isReplyAccepted(reply), // isSolution
                        null  // commentId
                    );
                } else {
                    return new UserActivityItemDto(
                        UserActivityItemDto.ActivityType.VOTE_CAST, 
                        vote.getCreatedAt(), 
                        reply.getScore(), 
                        (int) vote.getValue(), // voteValue
                        reply.getTopic().getId(), // questionId
                        reply.getTopic().getTitle(), // questionTitle
                        null, // answerId
                        false, // isSolution (comments can't be solutions)
                        reply.getId()  // commentId
                    );
                }
            }
            return null;
        }).filter(Objects::nonNull);

        // Combine and return
        return Stream.of(topicActivities, answerActivities, commentActivities, acceptedSolutionActivities, voteActivities)
                .flatMap(s -> s)
                .sorted(Comparator.comparing(UserActivityItemDto::createdAt).reversed())
                .toList();
    }

    // ... helper methods remain the same ...
    private boolean isAnswer(Reply reply) {
        return reply.getTopic().getTopicType() == TopicType.QUESTION && 
               reply.getParentReply() == null;
    }

    private boolean isComment(Reply reply) {
        return reply.getTopic().getTopicType() == TopicType.POST || 
               reply.getParentReply() != null;
    }

    private List<Reply> getAcceptedSolutionsByUser(Integer userId) {
        List<Topic> topicsWithUserSolutions = topicRepository.findByAcceptedSolution_Author_UserId(userId);
        return topicsWithUserSolutions.stream()
                .map(Topic::getAcceptedSolution)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isReplyAccepted(Reply reply) {
        return reply.getTopic().getAcceptedSolution() != null && 
               reply.getTopic().getAcceptedSolution().getId().equals(reply.getId());
    }
}