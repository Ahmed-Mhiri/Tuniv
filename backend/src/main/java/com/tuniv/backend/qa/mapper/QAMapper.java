package com.tuniv.backend.qa.mapper;

import java.util.Collections;
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
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.user.model.User; // ✅ ADD THIS LINE


public final class QAMapper {

    private QAMapper() {}

    /**
     * This is the main high-performance entry point for building the full question response.
     */
    public static QuestionResponseDto buildQuestionResponseDto(
            Question question,
            List<Answer> answers,
            Map<Integer, List<Comment>> commentsByAnswerId,
            UserDetailsImpl currentUser,
            Map<Integer, Integer> currentUserVotes
    ) {
        if (question == null) return null;

        List<AnswerResponseDto> answerDtos = answers.stream()
                .map(answer -> toAnswerResponseDto(
                        answer,
                        commentsByAnswerId.getOrDefault(answer.getId(), Collections.emptyList()),
                        currentUser,
                        currentUserVotes
                ))
                .sorted(Comparator.comparing(AnswerResponseDto::isSolution).reversed()
                        .thenComparing(AnswerResponseDto::score, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return new QuestionResponseDto(
                question.getId(),
                question.getTitle(),
                question.getBody(),
                question.getCreatedAt(),
                toAuthorDto(question.getAuthor()),
                toModuleDto(question.getModule()), // ✅ Module information is now included
                answerDtos,
                question.getScore(),
                currentUserVotes.getOrDefault(question.getId(), 0),
                question.getAttachments().stream().map(QAMapper::toAttachmentDto).collect(Collectors.toList())
        );
    }

    public static AnswerResponseDto toAnswerResponseDto(
            Answer answer,
            List<Comment> comments,
            UserDetailsImpl currentUser,
            Map<Integer, Integer> currentUserVotes
    ) {
        if (answer == null) return null;

        List<CommentResponseDto> commentDtos = comments.stream()
                .map(comment -> toCommentResponseDto(comment, currentUser, currentUserVotes))
                .sorted(Comparator.comparing(CommentResponseDto::score).reversed())
                .collect(Collectors.toList());

        return new AnswerResponseDto(
                answer.getId(),
                answer.getBody(),
                answer.getIsSolution(),
                answer.getCreatedAt(),
                toAuthorDto(answer.getAuthor()),
                answer.getScore(),
                currentUserVotes.getOrDefault(answer.getId(), 0),
                commentDtos,
                answer.getAttachments().stream().map(QAMapper::toAttachmentDto).collect(Collectors.toList())
        );
    }

    public static CommentResponseDto toCommentResponseDto(
            Comment comment,
            UserDetailsImpl currentUser,
            Map<Integer, Integer> currentUserVotes
    ) {
        if (comment == null) return null;

        return new CommentResponseDto(
                comment.getId(),
                comment.getBody(),
                comment.getCreatedAt(),
                toAuthorDto(comment.getAuthor()),
                comment.getScore(),
                currentUserVotes.getOrDefault(comment.getId(), 0),
                comment.getChildren().stream()
                        .map(child -> toCommentResponseDto(child, currentUser, currentUserVotes))
                        .sorted(Comparator.comparing(CommentResponseDto::score).reversed())
                        .collect(Collectors.toList()),
                comment.getAttachments().stream().map(QAMapper::toAttachmentDto).collect(Collectors.toList())
        );
    }

    public static AuthorDto toAuthorDto(User user) {
        if (user == null) return null;
        return new AuthorDto(user.getUserId(), user.getUsername(), user.getProfilePhotoUrl());
    }

    public static AttachmentDto toAttachmentDto(Attachment attachment) {
        if (attachment == null) return null;
        return new AttachmentDto(
                attachment.getAttachmentId(),
                attachment.getFileName(),
                attachment.getFileUrl(),
                attachment.getFileType()
        );
    }
    
    /**
    * ✅ NEW HELPER METHOD
    * Converts a Module entity into a ModuleDto.
    */
    public static ModuleDto toModuleDto(Module module) {
        if (module == null) return null;
        return new ModuleDto(module.getModuleId(), module.getName());
    }
}






