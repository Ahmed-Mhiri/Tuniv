package com.tuniv.backend.qa.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import com.tuniv.backend.user.model.User;

public final class QAMapper {

    private QAMapper() {}

    // ✅ CHANGE: Attachment maps are no longer needed in the signature.
    public static QuestionResponseDto toQuestionResponseDto(
            Question question,
            UserDetailsImpl currentUser,
            Map<Integer, Integer> scores,
            Map<Integer, Integer> currentUserVotes
    ) {
        if (question == null) return null;

        // ✅ CHANGE: Get attachments directly from the question object.
        List<AttachmentDto> attachments = question.getAttachments().stream()
                .map(QAMapper::toAttachmentDto)
                .collect(Collectors.toList());

        return new QuestionResponseDto(
                question.getId(), // ✅ CHANGE: Use getId()
                question.getTitle(),
                question.getBody(),
                question.getCreatedAt(),
                toAuthorDto(question.getAuthor()),
                question.getAnswers().stream()
                        // ✅ CHANGE: Pass fewer arguments to the nested mapper call.
                        .map(answer -> toAnswerResponseDto(answer, currentUser, scores, currentUserVotes))
                        .sorted(Comparator.comparing(AnswerResponseDto::isSolution).reversed()
                                .thenComparing(AnswerResponseDto::score, Comparator.reverseOrder()))
                        .collect(Collectors.toList()),
                scores.getOrDefault(question.getId(), 0), // ✅ CHANGE: Use getId()
                currentUserVotes.getOrDefault(question.getId(), 0), // ✅ CHANGE: Use getId()
                attachments
        );
    }

    // ✅ CHANGE: Attachment maps are no longer needed in the signature.
    public static AnswerResponseDto toAnswerResponseDto(
            Answer answer,
            UserDetailsImpl currentUser,
            Map<Integer, Integer> scores,
            Map<Integer, Integer> currentUserVotes
    ) {
        if (answer == null) return null;

        // ✅ CHANGE: Get attachments directly from the answer object.
        List<AttachmentDto> attachments = answer.getAttachments().stream()
                .map(QAMapper::toAttachmentDto)
                .collect(Collectors.toList());

        return new AnswerResponseDto(
                answer.getId(), // ✅ CHANGE: Use getId()
                answer.getBody(),
                answer.getIsSolution(),
                answer.getCreatedAt(),
                toAuthorDto(answer.getAuthor()),
                scores.getOrDefault(answer.getId(), 0), // ✅ CHANGE: Use getId()
                currentUserVotes.getOrDefault(answer.getId(), 0), // ✅ CHANGE: Use getId()
                answer.getComments().stream()
                        .filter(c -> c.getParentComment() == null)
                        // ✅ CHANGE: Pass fewer arguments to the nested mapper call.
                        .map(comment -> toCommentResponseDto(comment, currentUser, scores, currentUserVotes))
                        .sorted(Comparator.comparing(CommentResponseDto::score).reversed())
                        .collect(Collectors.toList()),
                attachments
        );
    }

    // ✅ CHANGE: Attachment map is no longer needed in the signature.
    public static CommentResponseDto toCommentResponseDto(
            Comment comment,
            UserDetailsImpl currentUser,
            Map<Integer, Integer> scores,
            Map<Integer, Integer> currentUserVotes
    ) {
        if (comment == null) return null;

        // ✅ CHANGE: Get attachments directly from the comment object.
        List<AttachmentDto> attachments = comment.getAttachments().stream()
                .map(QAMapper::toAttachmentDto)
                .collect(Collectors.toList());

        return new CommentResponseDto(
                comment.getId(), // ✅ CHANGE: Use getId()
                comment.getBody(),
                comment.getCreatedAt(),
                toAuthorDto(comment.getAuthor()),
                scores.getOrDefault(comment.getId(), 0), // ✅ CHANGE: Use getId()
                currentUserVotes.getOrDefault(comment.getId(), 0), // ✅ CHANGE: Use getId()
                comment.getChildren().stream()
                        // ✅ CHANGE: Pass fewer arguments to the nested mapper call.
                        .map(child -> toCommentResponseDto(child, currentUser, scores, currentUserVotes))
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
}