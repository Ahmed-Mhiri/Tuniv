package com.tuniv.backend.qa.mapper;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerResponseDto;
import com.tuniv.backend.qa.dto.AuthorDto;
import com.tuniv.backend.qa.dto.CommentResponseDto;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.model.Vote;
import com.tuniv.backend.user.model.User;

public final class QAMapper {

    // Private constructor to prevent instantiation
    private QAMapper() {}

    public static QuestionResponseDto toQuestionResponseDto(Question question, UserDetailsImpl currentUser) {
        if (question == null) return null;

        return new QuestionResponseDto(
            question.getQuestionId(),
            question.getTitle(),
            question.getBody(),
            question.getCreatedAt(),
            toAuthorDto(question.getAuthor()),
            question.getAnswers() != null ? question.getAnswers().stream()
                .map(answer -> toAnswerResponseDto(answer, currentUser))
                .sorted(Comparator.comparing(AnswerResponseDto::isSolution).reversed() // Show solution first
                        .thenComparing(AnswerResponseDto::score).reversed()) // Then sort by score
                .collect(Collectors.toList()) : Collections.emptyList(),
            calculateScore(question.getVotes()),
            getCurrentUserVote(question.getVotes(), currentUser)
        );
    }

    public static AnswerResponseDto toAnswerResponseDto(Answer answer, UserDetailsImpl currentUser) {
        if (answer == null) return null;

        return new AnswerResponseDto(
            answer.getAnswerId(),
            answer.getBody(),
            answer.getIsSolution(),
            answer.getCreatedAt(),
            toAuthorDto(answer.getAuthor()),
            calculateScore(answer.getVotes()),
            getCurrentUserVote(answer.getVotes(), currentUser),
            answer.getComments() != null ? answer.getComments().stream()
                .filter(comment -> comment.getParentComment() == null) // Get top-level comments only
                .map(comment -> toCommentResponseDto(comment, currentUser))
                .sorted(Comparator.comparing(CommentResponseDto::score).reversed()) // Sort comments by score
                .collect(Collectors.toList()) : Collections.emptyList()
        );
    }

    public static CommentResponseDto toCommentResponseDto(Comment comment, UserDetailsImpl currentUser) {
        if (comment == null) return null;
        
        return new CommentResponseDto(
            comment.getCommentId(),
            comment.getBody(),
            comment.getCreatedAt(),
            toAuthorDto(comment.getAuthor()),
            calculateScore(comment.getVotes()),
            getCurrentUserVote(comment.getVotes(), currentUser),
            comment.getChildren() != null ? comment.getChildren().stream()
                .map(child -> toCommentResponseDto(child, currentUser)) // Recursively map children
                .sorted(Comparator.comparing(CommentResponseDto::score).reversed())
                .collect(Collectors.toList()) : Collections.emptyList()
        );
    }

    public static AuthorDto toAuthorDto(User user) {
        if (user == null) return null;
        return new AuthorDto(user.getUserId(), user.getUsername());
    }

    // --- Generic Helper Methods for Vote Calculation ---

    private static <T extends Vote> int calculateScore(Set<T> votes) {
        if (votes == null) return 0;
        return votes.stream().mapToInt(Vote::getValue).sum();
    }

    private static <T extends Vote> int getCurrentUserVote(Set<T> votes, UserDetailsImpl currentUser) {
        if (currentUser == null || votes == null) return 0;
        return votes.stream()
            .filter(vote -> vote.getUser().getUserId().equals(currentUser.getId()))
            .mapToInt(Vote::getValue)
            .findFirst()
            .orElse(0);
    }
}