package com.tuniv.backend.qa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // <-- IMPORT ADDED
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.config.security.services.UserDetailsImpl; // <-- IMPORT ADDED
import com.tuniv.backend.qa.dto.AnswerCreateRequest;
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
        
        List<Attachment> savedAttachments = attachmentService.saveAttachments(files, savedQuestion.getQuestionId(), "QUESTION");
        savedQuestion.setAttachments(new HashSet<>(savedAttachments));

        Map<Integer, List<Attachment>> questionAttachments = savedAttachments.stream()
            .collect(Collectors.groupingBy(Attachment::getPostId));
        
        return QAMapper.toQuestionResponseDto(savedQuestion, currentUser, questionAttachments, Collections.emptyMap(), Collections.emptyMap());
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#questionId")
    public AnswerResponseDto addAnswer(AnswerCreateRequest request, Integer questionId, UserDetailsImpl currentUser, List<MultipartFile> files) {
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

        List<Attachment> savedAttachments = attachmentService.saveAttachments(files, savedAnswer.getAnswerId(), "ANSWER");
        savedAnswer.setAttachments(new HashSet<>(savedAttachments));
        
        NewAnswerEvent event = new NewAnswerEvent(this, question.getTitle(), question.getAuthor().getEmail(), author.getUsername());
        eventPublisher.publishEvent(event);

        Map<Integer, List<Attachment>> answerAttachments = savedAttachments.stream()
            .collect(Collectors.groupingBy(Attachment::getPostId));

        return QAMapper.toAnswerResponseDto(savedAnswer, currentUser, answerAttachments, Collections.emptyMap());
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponseDto> getQuestionsByModule(Integer moduleId, Pageable pageable, UserDetailsImpl currentUser) {
        Page<Question> questionPage = questionRepository.findByModuleModuleId(moduleId, pageable);
        // NOTE: For performance, this list view does not fetch attachments.
        // They are fetched in the detailed getQuestionById view.
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