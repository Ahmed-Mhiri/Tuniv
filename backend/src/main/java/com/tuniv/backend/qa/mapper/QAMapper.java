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
     * ✅ NEW PRIMARY METHOD
     * Builds the full question response from a single, fully-populated Question entity.
     * This is the new high-performance entry point, designed to work with the Entity Graph fetch.
     *
     * @param question         The fully-loaded Question entity, including its answers, comments, and replies.
     * @param currentUser      The details of the user making the request, for vote calculation.
     * @param currentUserVotes A map of post IDs to the current user's vote value (1, -1, or 0).
     * @return A complete QuestionResponseDto.
     */
    public static QuestionResponseDto buildQuestionResponseDto(
            Question question,
            UserDetailsImpl currentUser,
            Map<Integer, Integer> currentUserVotes
    ) {
        if (question == null) return null;

        // Map the answers directly from the question entity. The 'comments' are already fetched.
        List<AnswerResponseDto> answerDtos = question.getAnswers().stream()
                .map(answer -> toAnswerResponseDto(answer, currentUser, currentUserVotes))
                .sorted(Comparator.comparing(AnswerResponseDto::isSolution).reversed()
                        .thenComparing(AnswerResponseDto::score, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return new QuestionResponseDto(
                question.getId(),
                question.getTitle(),
                question.getBody(),
                question.getCreatedAt(),
                toAuthorDto(question.getAuthor()),
                toModuleDto(question.getModule()),
                answerDtos,
                question.getScore(),
                currentUserVotes.getOrDefault(question.getId(), 0),
                question.getAttachments().stream().map(QAMapper::toAttachmentDto).collect(Collectors.toList())
        );
    }

    /**
     * Maps an Answer entity to its DTO representation.
     * It now directly uses the 'comments' collection from the Answer entity.
     */
    public static AnswerResponseDto toAnswerResponseDto(
            Answer answer,
            UserDetailsImpl currentUser,
            Map<Integer, Integer> currentUserVotes
    ) {
        if (answer == null) return null;

        List<CommentResponseDto> commentDtos = answer.getComments().stream()
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

    /**
     * Recursively maps a Comment entity and its children to their DTO representation.
     * This method remains unchanged as its recursive nature is perfect for the entity graph.
     */
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
     * ✅ UPDATED: Now matches the new ModuleDto constructor with 4 parameters
     * Note: For question responses, we don't have the current user context for isMember,
     * so we default to false. The question count is taken from the module entity.
     */
    public static ModuleDto toModuleDto(Module module) {
        if (module == null) return null;
        return new ModuleDto(
            module.getModuleId(), 
            module.getName(),
            module.getQuestionCount(), // ✅ Added question count
            false // ✅ Default to false since we don't have user context here
        );
    }

    /**
     * ✅ NEW OVERLOAD: For cases where we have user context
     */
    public static ModuleDto toModuleDto(Module module, boolean isMember) {
        if (module == null) return null;
        return new ModuleDto(
            module.getModuleId(), 
            module.getName(),
            module.getQuestionCount(), // ✅ Added question count
            isMember // ✅ Use provided member status
        );
    }

    /**
     * @deprecated This method is kept for backward compatibility (e.g., in createQuestion)
     * but the new, simpler buildQuestionResponseDto(Question, ...) is preferred for performance.
     */
    @Deprecated
    public static QuestionResponseDto buildQuestionResponseDto(
            Question question,
            List<Answer> answers,
            Map<Integer, List<Comment>> commentsByAnswerId,
            UserDetailsImpl currentUser,
            Map<Integer, Integer> currentUserVotes
    ) {
        if (question == null) return null;

        List<AnswerResponseDto> answerDtos = answers.stream()
                .map(answer -> {
                    // This is the key difference: comments are looked up from the map.
                    List<Comment> commentsForAnswer = commentsByAnswerId.getOrDefault(answer.getId(), Collections.emptyList());
                    List<CommentResponseDto> commentDtos = commentsForAnswer.stream()
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
                })
                .sorted(Comparator.comparing(AnswerResponseDto::isSolution).reversed()
                        .thenComparing(AnswerResponseDto::score, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return new QuestionResponseDto(
                question.getId(),
                question.getTitle(),
                question.getBody(),
                question.getCreatedAt(),
                toAuthorDto(question.getAuthor()),
                toModuleDto(question.getModule()), // ✅ Now uses the updated method
                answerDtos,
                question.getScore(),
                currentUserVotes.getOrDefault(question.getId(), 0),
                question.getAttachments().stream().map(QAMapper::toAttachmentDto).collect(Collectors.toList())
        );
    }
}