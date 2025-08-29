package com.tuniv.backend.qa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service; // <-- IMPORT ADDED
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerCreateRequest; // <-- IMPORT ADDED
import com.tuniv.backend.qa.dto.AnswerResponseDto;
import com.tuniv.backend.qa.dto.QuestionCreateRequest;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.event.NewAnswerEvent;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.AttachmentRepository;
import com.tuniv.backend.qa.repository.QuestionRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;

    @Transactional
    public QuestionResponseDto createQuestion(QuestionCreateRequest request, Integer moduleId, UserDetailsImpl currentUser, List<MultipartFile> files) {
        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Module not found with id: " + moduleId));

        Question question = new Question();
        question.setTitle(request.title());
        question.setBody(request.body());
        question.setModule(module);
        question.setAuthor(author);

        Question savedQuestion = questionRepository.save(question);
        attachmentService.saveAttachments(files, savedQuestion.getQuestionId(), "QUESTION");

        Question finalQuestion = questionRepository.findById(savedQuestion.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Failed to re-fetch question"));

        Map<Integer, List<Attachment>> questionAttachments = finalQuestion.getAttachments().stream()
                .collect(Collectors.groupingBy(Attachment::getPostId));

        return QAMapper.toQuestionResponseDto(finalQuestion, currentUser, questionAttachments, Collections.emptyMap(), Collections.emptyMap());
    }

    @Transactional
@CacheEvict(value = "questions", key = "#questionId")
public AnswerResponseDto addAnswer(AnswerCreateRequest request, Integer questionId, UserDetailsImpl currentUser, List<MultipartFile> files) {
    // =========================================================================
    // ✅ FINAL VALIDATION: Ensure the submission is not completely empty.
    // =========================================================================
    boolean isBodyEmpty = request.body() == null || request.body().trim().isEmpty();
    boolean hasFiles = files != null && !files.stream().allMatch(MultipartFile::isEmpty);

    if (isBodyEmpty && !hasFiles) {
        throw new IllegalArgumentException("Cannot create an empty answer. Please provide text or attach a file.");
    }
    // =========================================================================

    User author = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
    Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new ResourceNotFoundException("Error: Question not found with id: " + questionId));

    Answer answer = new Answer();
    answer.setBody(request.body());
    answer.setQuestion(question);
    answer.setAuthor(author);
    question.getAnswers().add(answer);

    Answer savedAnswer = answerRepository.save(answer);
    attachmentService.saveAttachments(files, savedAnswer.getAnswerId(), "ANSWER");
    
    NewAnswerEvent event = new NewAnswerEvent(this, question.getTitle(), question.getAuthor().getEmail(), author.getUsername());
    eventPublisher.publishEvent(event);

    Answer finalAnswer = answerRepository.findById(savedAnswer.getAnswerId())
            .orElseThrow(() -> new ResourceNotFoundException("Failed to re-fetch answer"));

    Map<Integer, List<Attachment>> answerAttachments = finalAnswer.getAttachments().stream()
            .collect(Collectors.groupingBy(Attachment::getPostId));

    return QAMapper.toAnswerResponseDto(finalAnswer, currentUser, answerAttachments, Collections.emptyMap());
}

    @Transactional(readOnly = true)
    public Page<QuestionResponseDto> getQuestionsByModule(Integer moduleId, Pageable pageable, UserDetailsImpl currentUser) {
        Page<Question> questionPage = questionRepository.findByModuleModuleId(moduleId, pageable);
        return questionPage.map(question -> QAMapper.toQuestionResponseDto(question, currentUser,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "questions", key = "#questionId")
    public QuestionResponseDto getQuestionById(Integer questionId, UserDetailsImpl currentUser) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        List<Integer> answerIds = question.getAnswers().stream().map(Answer::getAnswerId).collect(Collectors.toList());
        List<Integer> commentIds = question.getAnswers().stream()
                .flatMap(answer -> answer.getComments().stream())
                .flatMap(comment -> flattenComments(comment).stream())
                .map(Comment::getCommentId)
                .collect(Collectors.toList());

        Map<Integer, List<Attachment>> questionAttachments = findAttachments("QUESTION", List.of(questionId));
        Map<Integer, List<Attachment>> answerAttachments = findAttachments("ANSWER", answerIds);
        Map<Integer, List<Attachment>> commentAttachments = findAttachments("COMMENT", commentIds);

        return QAMapper.toQuestionResponseDto(question, currentUser, questionAttachments, answerAttachments, commentAttachments);
    }

    private List<Comment> flattenComments(Comment comment) {
        List<Comment> list = new ArrayList<>();
        list.add(comment);
        comment.getChildren().forEach(child -> list.addAll(flattenComments(child)));
        return list;
    }

    private Map<Integer, List<Attachment>> findAttachments(String postType, List<Integer> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return attachmentRepository.findAllByPostTypeAndPostIdIn(postType, postIds)
                .stream().collect(Collectors.groupingBy(Attachment::getPostId));
    }
}
