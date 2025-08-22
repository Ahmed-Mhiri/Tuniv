package com.tuniv.backend.qa.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerCreateRequest;
import com.tuniv.backend.qa.dto.QuestionCreateRequest;
import com.tuniv.backend.qa.event.NewAnswerEvent;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.repository.AnswerRepository;
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

    @Transactional
    public Question createQuestion(QuestionCreateRequest request, Integer moduleId, UserDetailsImpl currentUser, List<MultipartFile> files) {
        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Module not found with id: " + moduleId));

        Question question = new Question();
        question.setTitle(request.title());
        question.setBody(request.body());
        question.setModule(module);
        question.setAuthor(author);
        question.setCreatedAt(LocalDateTime.now());

        Question savedQuestion = questionRepository.save(question);

        attachmentService.saveAttachments(files, savedQuestion.getQuestionId(), "QUESTION");

        return savedQuestion;
    }

    @Transactional
    public Answer addAnswer(AnswerCreateRequest request, Integer questionId, UserDetailsImpl currentUser, List<MultipartFile> files) {
        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Question not found with id: " + questionId));

        Answer answer = new Answer();
        answer.setBody(request.body());
        answer.setQuestion(question);
        answer.setAuthor(author);
        answer.setIsSolution(false);
        answer.setCreatedAt(LocalDateTime.now());

        Answer savedAnswer = answerRepository.save(answer);

        attachmentService.saveAttachments(files, savedAnswer.getAnswerId(), "ANSWER");

        NewAnswerEvent event = new NewAnswerEvent(
            this,
            question.getTitle(),
            question.getAuthor().getEmail(),
            author.getUsername()
        );
        eventPublisher.publishEvent(event);

        return savedAnswer;
    }

    public List<Question> getQuestionsByModule(Integer moduleId) {
        return questionRepository.findByModuleModuleId(moduleId);
    }
    
    // All voting methods have been moved to VoteService.java
}