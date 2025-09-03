package com.tuniv.backend.user.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.qa.model.Vote;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.AnswerVoteRepository;
import com.tuniv.backend.qa.repository.CommentRepository;
import com.tuniv.backend.qa.repository.CommentVoteRepository;
import com.tuniv.backend.qa.repository.QuestionRepository;
import com.tuniv.backend.qa.repository.QuestionVoteRepository;
import com.tuniv.backend.user.dto.UserActivityItemDto;
import com.tuniv.backend.user.dto.UserActivityItemDto.ActivityType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CommentRepository commentRepository;
    private final QuestionVoteRepository questionVoteRepository;
    private final AnswerVoteRepository answerVoteRepository;
    private final CommentVoteRepository commentVoteRepository;

    private int calculatePostScore(Stream<? extends Vote> votes) {
        return votes.mapToInt(v -> (int) v.getValue()).sum();
    }

    @Transactional(readOnly = true)
    public List<UserActivityItemDto> getActivityForUser(Integer userId) {
        // --- 1. Fetch all posts, accepted answers, and votes for the user ---
        var questions = questionRepository.findByAuthorUserIdOrderByCreatedAtDesc(userId);
        var answers = answerRepository.findByAuthorUserIdOrderByCreatedAtDesc(userId);
        var comments = commentRepository.findByAuthorUserIdOrderByCreatedAtDesc(userId);
        var acceptedAnswers = answerRepository.findByQuestionAuthorUserIdAndIsSolutionTrueOrderByUpdatedAtDesc(userId);
        var questionVotes = questionVoteRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        var answerVotes = answerVoteRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        var commentVotes = commentVoteRepository.findByUserUserIdOrderByCreatedAtDesc(userId);

        // --- 2. Convert each list into a stream of standardized DTOs ---
        Stream<UserActivityItemDto> questionActivities = questions.stream()
                .map(q -> new UserActivityItemDto(ActivityType.QUESTION_ASKED, q.getCreatedAt(), calculatePostScore(q.getVotes().stream()), null, q.getId(), q.getTitle(), null, false, null)); // ✅ FIX

        Stream<UserActivityItemDto> answerActivities = answers.stream()
                .map(a -> new UserActivityItemDto(ActivityType.ANSWER_POSTED, a.getCreatedAt(), calculatePostScore(a.getVotes().stream()), null, a.getQuestion().getId(), a.getQuestion().getTitle(), a.getId(), a.getIsSolution(), null)); // ✅ FIX (x2)

        Stream<UserActivityItemDto> commentActivities = comments.stream()
                .map(c -> new UserActivityItemDto(ActivityType.COMMENT_POSTED, c.getCreatedAt(), calculatePostScore(c.getVotes().stream()), null, c.getAnswer().getQuestion().getId(), c.getAnswer().getQuestion().getTitle(), c.getAnswer().getId(), false, c.getId())); // ✅ FIX (x3)

        Stream<UserActivityItemDto> acceptedAnswerActivities = acceptedAnswers.stream()
                .map(a -> new UserActivityItemDto(ActivityType.ACCEPTED_AN_ANSWER, a.getUpdatedAt(), calculatePostScore(a.getVotes().stream()), null, a.getQuestion().getId(), a.getQuestion().getTitle(), a.getId(), true, null)); // ✅ FIX (x2)

        Stream<UserActivityItemDto> voteActivities = Stream.of(
            questionVotes.stream().map(v -> new UserActivityItemDto(ActivityType.VOTE_CAST, v.getCreatedAt(), calculatePostScore(v.getQuestion().getVotes().stream()), (int) v.getValue(), v.getQuestion().getId(), v.getQuestion().getTitle(), null, false, null)), // ✅ FIX
            answerVotes.stream().map(v -> new UserActivityItemDto(ActivityType.VOTE_CAST, v.getCreatedAt(), calculatePostScore(v.getAnswer().getVotes().stream()), (int) v.getValue(), v.getAnswer().getQuestion().getId(), v.getAnswer().getQuestion().getTitle(), v.getAnswer().getId(), v.getAnswer().getIsSolution(), null)), // ✅ FIX (x2)
            commentVotes.stream().map(v -> new UserActivityItemDto(ActivityType.VOTE_CAST, v.getCreatedAt(), calculatePostScore(v.getComment().getVotes().stream()), (int) v.getValue(), v.getComment().getAnswer().getQuestion().getId(), v.getComment().getAnswer().getQuestion().getTitle(), v.getComment().getAnswer().getId(), false, v.getComment().getId())) // ✅ FIX (x3)
        ).flatMap(s -> s);

        // --- 3. Combine all streams, sort by date, and collect into a list ---
        return Stream.of(questionActivities, answerActivities, commentActivities, acceptedAnswerActivities, voteActivities)
                .flatMap(s -> s)
                .sorted(Comparator.comparing(UserActivityItemDto::createdAt).reversed())
                .toList();
    }
}