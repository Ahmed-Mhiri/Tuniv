package com.tuniv.backend.qa.mapper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerResponseDto;
import com.tuniv.backend.qa.dto.AttachmentDto;
import com.tuniv.backend.qa.dto.AuthorDto;
import com.tuniv.backend.qa.dto.CommentResponseDto;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.model.Vote;
import com.tuniv.backend.user.model.User;

public final class QAMapper {

    private QAMapper() {}

    public static QuestionResponseDto toQuestionResponseDto(
            Question question,
            UserDetailsImpl currentUser,
            Map<Integer, List<Attachment>> questionAttachments,
            Map<Integer, List<Attachment>> answerAttachments,
            Map<Integer, List<Attachment>> commentAttachments) {
        
        if (question == null) return null;

        List<AttachmentDto> attachments = questionAttachments.getOrDefault(question.getQuestionId(), Collections.emptyList())
                .stream().map(QAMapper::toAttachmentDto).collect(Collectors.toList());

        return new QuestionResponseDto(
            question.getQuestionId(),
            question.getTitle(),
            question.getBody(),
            question.getCreatedAt(),
            toAuthorDto(question.getAuthor()),
            question.getAnswers().stream()
                .map(answer -> toAnswerResponseDto(answer, currentUser, answerAttachments, commentAttachments))
                .sorted(Comparator.comparing(AnswerResponseDto::isSolution).reversed()
                        .thenComparing(AnswerResponseDto::score, Comparator.reverseOrder()))
                .collect(Collectors.toList()),
            calculateScore(question.getVotes()),
            getCurrentUserVote(question.getVotes(), currentUser),
            attachments
        );
    }

    public static AnswerResponseDto toAnswerResponseDto(
            Answer answer,
            UserDetailsImpl currentUser,
            Map<Integer, List<Attachment>> answerAttachments,
            Map<Integer, List<Attachment>> commentAttachments) {
        
        if (answer == null) return null;

        List<AttachmentDto> attachments = answerAttachments.getOrDefault(answer.getAnswerId(), Collections.emptyList())
            .stream().map(QAMapper::toAttachmentDto).collect(Collectors.toList());

        return new AnswerResponseDto(
            answer.getAnswerId(),
            answer.getBody(),
            answer.getIsSolution(),
            answer.getCreatedAt(),
            toAuthorDto(answer.getAuthor()),
            calculateScore(answer.getVotes()),
            getCurrentUserVote(answer.getVotes(), currentUser),
            answer.getComments().stream()
                .filter(c -> c.getParentComment() == null)
                .map(comment -> toCommentResponseDto(comment, currentUser, commentAttachments))
                .sorted(Comparator.comparing(CommentResponseDto::score).reversed())
                .collect(Collectors.toList()),
            attachments
        );
    }

    public static CommentResponseDto toCommentResponseDto(
            Comment comment,
            UserDetailsImpl currentUser,
            Map<Integer, List<Attachment>> commentAttachments) {
        
        if (comment == null) return null;
        
        List<AttachmentDto> attachments = commentAttachments.getOrDefault(comment.getCommentId(), Collections.emptyList())
            .stream().map(QAMapper::toAttachmentDto).collect(Collectors.toList());

        return new CommentResponseDto(
            comment.getCommentId(),
            comment.getBody(),
            comment.getCreatedAt(),
            toAuthorDto(comment.getAuthor()),
            calculateScore(comment.getVotes()),
            getCurrentUserVote(comment.getVotes(), currentUser),
            comment.getChildren().stream()
                .map(child -> toCommentResponseDto(child, currentUser, commentAttachments))
                .sorted(Comparator.comparing(CommentResponseDto::score).reversed())
                .collect(Collectors.toList()),
            attachments
        );
    }

    public static AuthorDto toAuthorDto(User user) {
        if (user == null) return null;
        return new AuthorDto(user.getUserId(), user.getUsername(), user.getProfilePhotoUrl());
    }

    public static AttachmentDto toAttachmentDto(Attachment attachment) {
        if (attachment == null) return null;
        return new AttachmentDto(
            attachment.getFileName(),
            attachment.getFileUrl(),
            attachment.getFileType()
        );
    }

    private static <T extends Vote> int calculateScore(Set<T> votes) {
        if (votes == null) return 0;
        return votes.stream().mapToInt(v -> (int) v.getValue()).sum();
    }

    private static <T extends Vote> int getCurrentUserVote(Set<T> votes, UserDetailsImpl currentUser) {
        if (currentUser == null || votes == null) return 0;
        return votes.stream()
            .filter(vote -> vote.getUser().getUserId().equals(currentUser.getId()))
            .mapToInt(v -> (int) v.getValue())
            .findFirst()
            .orElse(0);
    }
}