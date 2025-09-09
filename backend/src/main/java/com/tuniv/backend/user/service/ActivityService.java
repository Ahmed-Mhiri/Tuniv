package com.tuniv.backend.user.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.CommentRepository;
import com.tuniv.backend.qa.repository.QuestionRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.user.dto.UserActivityItemDto;
import com.tuniv.backend.user.dto.UserActivityItemDto.ActivityType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository; // ✅ REPLACED 3 REPOS WITH 1

    @Transactional(readOnly = true)
    public List<UserActivityItemDto> getActivityForUser(Integer userId) {
        // --- 1. Fetch all posts, accepted answers, and votes for the user ---
        var questions = questionRepository.findByAuthor_IdOrderByCreatedAtDesc(userId);
        var answers = answerRepository.findByAuthor_IdOrderByCreatedAtDesc(userId);
        var comments = commentRepository.findByAuthor_IdOrderByCreatedAtDesc(userId);
        var acceptedAnswers = answerRepository.findByQuestion_Author_IdAndIsSolutionTrueOrderByUpdatedAtDesc(userId);
        // ✅ FETCH ALL VOTES IN A SINGLE CALL
        var votes = voteRepository.findByUser_IdOrderByCreatedAtDesc(userId);

        // --- 2. Convert each list into a stream of standardized DTOs ---
        Stream<UserActivityItemDto> questionActivities = questions.stream()
                .map(q -> new UserActivityItemDto(ActivityType.QUESTION_ASKED, q.getCreatedAt(), q.getScore(), null, q.getId(), q.getTitle(), null, false, null));

        Stream<UserActivityItemDto> answerActivities = answers.stream()
                .map(a -> new UserActivityItemDto(ActivityType.ANSWER_POSTED, a.getCreatedAt(), a.getScore(), null, a.getQuestion().getId(), a.getQuestion().getTitle(), a.getId(), a.getIsSolution(), null));

        Stream<UserActivityItemDto> commentActivities = comments.stream()
                .map(c -> new UserActivityItemDto(ActivityType.COMMENT_POSTED, c.getCreatedAt(), c.getScore(), null, c.getAnswer().getQuestion().getId(), c.getAnswer().getQuestion().getTitle(), c.getAnswer().getId(), false, c.getId()));

        Stream<UserActivityItemDto> acceptedAnswerActivities = acceptedAnswers.stream()
                .map(a -> new UserActivityItemDto(ActivityType.ACCEPTED_AN_ANSWER, a.getUpdatedAt(), a.getScore(), null, a.getQuestion().getId(), a.getQuestion().getTitle(), a.getId(), true, null));

        // ✅ NEW LOGIC: Map the single stream of votes, checking the type of post
        Stream<UserActivityItemDto> voteActivities = votes.stream().map(vote -> {
            Post post = vote.getPost();
            if (post instanceof Question q) {
                return new UserActivityItemDto(ActivityType.VOTE_CAST, vote.getCreatedAt(), q.getScore(), (int) vote.getValue(), q.getId(), q.getTitle(), null, false, null);
            } else if (post instanceof Answer a) {
                return new UserActivityItemDto(ActivityType.VOTE_CAST, vote.getCreatedAt(), a.getScore(), (int) vote.getValue(), a.getQuestion().getId(), a.getQuestion().getTitle(), a.getId(), a.getIsSolution(), null);
            } else if (post instanceof Comment c) {
                return new UserActivityItemDto(ActivityType.VOTE_CAST, vote.getCreatedAt(), c.getScore(), (int) vote.getValue(), c.getAnswer().getQuestion().getId(), c.getAnswer().getQuestion().getTitle(), c.getAnswer().getId(), false, c.getId());
            }
            return null; // Should not happen
        }).filter(Objects::nonNull);

        // --- 3. Combine all streams, sort by date, and collect into a list ---
        return Stream.of(questionActivities, answerActivities, commentActivities, acceptedAnswerActivities, voteActivities)
                .flatMap(s -> s)
                .sorted(Comparator.comparing(UserActivityItemDto::createdAt).reversed())
                .toList();
    }
}