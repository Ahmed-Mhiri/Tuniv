package com.tuniv.backend.qa.mapper;

import com.tuniv.backend.qa.dto.AnswerResponseDto;
import com.tuniv.backend.qa.dto.AuthorDto;
import com.tuniv.backend.qa.dto.CommentResponseDto;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.user.model.User;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class QAMapper {

    public static QuestionResponseDto toQuestionResponseDto(Question question) {
        if (question == null) return null;

        return new QuestionResponseDto(
                question.getQuestionId(),
                question.getTitle(),
                question.getBody(),
                question.getCreatedAt(),
                toAuthorDto(question.getAuthor()),
                question.getAnswers() != null ? question.getAnswers().stream()
                        .map(QAMapper::toAnswerResponseDto)
                        .collect(Collectors.toList()) : Collections.emptyList()
        );
    }

    public static AnswerResponseDto toAnswerResponseDto(Answer answer) {
        if (answer == null) return null;

        return new AnswerResponseDto(
                answer.getAnswerId(),
                answer.getBody(),
                answer.getIsSolution(),
                answer.getCreatedAt(),
                toAuthorDto(answer.getAuthor())
        );
    }

    public static CommentResponseDto toCommentResponseDto(Comment comment) {
        if (comment == null) return null;
        
        return new CommentResponseDto(
                comment.getCommentId(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getAuthor().getUsername()
        );
    }

    public static AuthorDto toAuthorDto(User user) {
        if (user == null) return null;
        
        return new AuthorDto(
                user.getUserId(),
                user.getUsername()
        );
    }
}