package com.tuniv.backend.qa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher; // Import all models
import org.springframework.data.domain.Page; // Import all repositories
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.auth.service.PostAuthorizationService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerCreateRequest;
import com.tuniv.backend.qa.dto.AnswerResponseDto;
import com.tuniv.backend.qa.dto.QuestionCreateRequest;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.dto.QuestionUpdateRequest;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.AnswerVote;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.CommentVote;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.model.QuestionVote;
import com.tuniv.backend.qa.model.Vote;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.AnswerVoteRepository;
import com.tuniv.backend.qa.repository.CommentVoteRepository;
import com.tuniv.backend.qa.repository.QuestionRepository;
import com.tuniv.backend.qa.repository.QuestionVoteRepository;
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
    private final QuestionVoteRepository questionVoteRepository;
    private final AnswerVoteRepository answerVoteRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final PostAuthorizationService postAuthorizationService;

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
        attachmentService.saveAttachments(files, savedQuestion);

        // eventPublisher.publishEvent(new NewQuestionInUniversityEvent(this, savedQuestion)); // Uncomment if you have this event

        return QAMapper.toQuestionResponseDto(savedQuestion, currentUser,
                Collections.emptyMap(), Collections.emptyMap());
    }

    @Transactional
    @CacheEvict(value = "questions", allEntries = true) // Evict all for simplicity, or target specific questionId
    public AnswerResponseDto addAnswer(AnswerCreateRequest request, Integer questionId, UserDetailsImpl currentUser, List<MultipartFile> files) {
        boolean isBodyEmpty = request.body() == null || request.body().trim().isEmpty();
        boolean hasFiles = files != null && !files.isEmpty() && files.stream().anyMatch(f -> f.getSize() > 0);

        if (isBodyEmpty && !hasFiles) {
            throw new IllegalArgumentException("Cannot create an empty answer. Please provide text or attach a file.");
        }

        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Question not found with id: " + questionId));

        Answer answer = new Answer();
        answer.setBody(request.body());
        answer.setQuestion(question);
        answer.setAuthor(author);

        Answer savedAnswer = answerRepository.save(answer);
        attachmentService.saveAttachments(files, savedAnswer);

        // eventPublisher.publishEvent(new NewAnswerEvent(this, savedAnswer)); // Uncomment if you have this event

        return QAMapper.toAnswerResponseDto(savedAnswer, currentUser,
                Collections.emptyMap(), Collections.emptyMap());
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#questionId")
    public QuestionResponseDto updateQuestion(Integer questionId, QuestionUpdateRequest request, List<MultipartFile> newFiles, UserDetailsImpl currentUser) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        postAuthorizationService.checkOwnership(question, currentUser);
        
        question.setTitle(request.title());
        question.setBody(request.body());

        // ✨ --- REFACTORED DELETION LOGIC --- ✨
        if (request.attachmentIdsToDelete() != null && !request.attachmentIdsToDelete().isEmpty()) {
            Set<Attachment> toDelete = question.getAttachments().stream()
                    .filter(att -> request.attachmentIdsToDelete().contains(att.getAttachmentId()))
                    .collect(Collectors.toSet());
            
            // Delete physical files first
            attachmentService.deleteAttachments(toDelete);
            
            // Use the helper method to ensure both sides of the relationship are updated in memory
            toDelete.forEach(question::removeAttachment);
        }

        // Add new attachments using the helper method inside this service
        attachmentService.saveAttachments(newFiles, question);

        Question updatedQuestion = questionRepository.save(question);
        
        // Note: For a complete DTO, you'd need to fetch votes here.
        return QAMapper.toQuestionResponseDto(updatedQuestion, currentUser, Collections.emptyMap(), Collections.emptyMap());
    }

    @Transactional
    public void deleteQuestion(Integer questionId, UserDetailsImpl currentUser) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        
        postAuthorizationService.checkOwnership(question, currentUser);
        
        // We must trigger the physical file cleanup before the entity is deleted.
        // JPA's orphanRemoval will handle deleting all the database records.
        Set<Attachment> allAttachmentsToDelete = new HashSet<>(question.getAttachments());
        question.getAnswers().forEach(answer -> {
            allAttachmentsToDelete.addAll(answer.getAttachments());
            answer.getComments().forEach(comment -> allAttachmentsToDelete.addAll(comment.getAttachments()));
        });

        attachmentService.deleteAttachments(allAttachmentsToDelete);

        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponseDto> getQuestionsByModule(Integer moduleId, Pageable pageable, UserDetailsImpl currentUser) {
        Page<Question> questionPage = questionRepository.findByModuleModuleId(moduleId, pageable);
        List<Question> questions = questionPage.getContent();
        if (questions.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Integer> questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());
        List<QuestionVote> votes = questionVoteRepository.findByQuestionIdIn(questionIds);

        Map<Integer, Integer> scores = votes.stream().collect(Collectors.groupingBy(
                vote -> vote.getQuestion().getId(),
                Collectors.summingInt(vote -> (int) vote.getValue())
        ));
        Map<Integer, Integer> currentUserVotes = votes.stream()
                .filter(vote -> vote.getUser().getUserId().equals(currentUser.getId()))
                .collect(Collectors.toMap(vote -> vote.getQuestion().getId(), vote -> (int) vote.getValue()));

        return questionPage.map(question -> QAMapper.toQuestionResponseDto(question, currentUser, scores, currentUserVotes));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "questions", key = "#questionId")
    public QuestionResponseDto getQuestionById(Integer questionId, UserDetailsImpl currentUser) {
        Question question = questionRepository.findByIdWithDetails(questionId) // Assuming an optimized fetch method
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        List<Integer> answerIds = question.getAnswers().stream().map(Answer::getId).collect(Collectors.toList());
        List<Integer> commentIds = question.getAnswers().stream()
                .flatMap(answer -> answer.getComments().stream())
                .flatMap(comment -> flattenComments(comment).stream())
                .map(Comment::getId)
                .collect(Collectors.toList());

        List<QuestionVote> questionVotes = questionVoteRepository.findByQuestionIdIn(List.of(questionId));
        List<AnswerVote> answerVotes = answerIds.isEmpty() ? Collections.emptyList() : answerVoteRepository.findByAnswerIdIn(answerIds);
        List<CommentVote> commentVotes = commentIds.isEmpty() ? Collections.emptyList() : commentVoteRepository.findByCommentIdIn(commentIds);

        Map<Integer, Integer> scores = Stream.of(questionVotes, answerVotes, commentVotes)
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(Vote::getPostId, Collectors.summingInt(vote -> (int) vote.getValue())));

        Map<Integer, Integer> currentUserVotes = Stream.of(questionVotes, answerVotes, commentVotes)
                .flatMap(List::stream)
                .filter(vote -> currentUser != null && vote.getUser().getUserId().equals(currentUser.getId()))
                .collect(Collectors.toMap(Vote::getPostId, vote -> (int) vote.getValue(), (v1, v2) -> v1));

        return QAMapper.toQuestionResponseDto(question, currentUser, scores, currentUserVotes);
    }

    private List<Comment> flattenComments(Comment comment) {
        List<Comment> list = new ArrayList<>();
        list.add(comment);
        if (comment.getChildren() != null) {
            comment.getChildren().forEach(child -> list.addAll(flattenComments(child)));
        }
        return list;
    }
}