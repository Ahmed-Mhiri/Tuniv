package com.tuniv.backend.qa.service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewAnswerEvent;
import com.tuniv.backend.notification.event.NewQuestionInUniversityEvent;
import com.tuniv.backend.qa.dto.AnswerCreateRequest;
import com.tuniv.backend.qa.dto.AnswerResponseDto;
import com.tuniv.backend.qa.dto.QuestionCreateRequest;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.*; // Import all models
import com.tuniv.backend.qa.repository.*; // Import all repositories
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // --- 1. Inject all vote repositories for bulk fetching ---
    private final QuestionVoteRepository questionVoteRepository;
    private final AnswerVoteRepository answerVoteRepository;
    private final CommentVoteRepository commentVoteRepository;

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

        eventPublisher.publishEvent(new NewQuestionInUniversityEvent(this, finalQuestion));

        // --- 2. Call updated mapper. For new questions, scores and votes are empty. ---
        return QAMapper.toQuestionResponseDto(finalQuestion, currentUser, questionAttachments,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#questionId")
    public AnswerResponseDto addAnswer(AnswerCreateRequest request, Integer questionId, UserDetailsImpl currentUser, List<MultipartFile> files) {
        boolean isBodyEmpty = request.body() == null || request.body().trim().isEmpty();
        boolean hasFiles = files != null && !files.stream().allMatch(MultipartFile::isEmpty);

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
        attachmentService.saveAttachments(files, savedAnswer.getAnswerId(), "ANSWER");
        
        Answer finalAnswer = answerRepository.findById(savedAnswer.getAnswerId())
                .orElseThrow(() -> new ResourceNotFoundException("Failed to re-fetch answer"));

        Map<Integer, List<Attachment>> answerAttachments = finalAnswer.getAttachments().stream()
                .collect(Collectors.groupingBy(Attachment::getPostId));

        eventPublisher.publishEvent(new NewAnswerEvent(this, finalAnswer));

        // --- 3. Call updated mapper. For new answers, scores and votes are empty. ---
        return QAMapper.toAnswerResponseDto(finalAnswer, currentUser, answerAttachments,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponseDto> getQuestionsByModule(Integer moduleId, Pageable pageable, UserDetailsImpl currentUser) {
        Page<Question> questionPage = questionRepository.findByModuleModuleId(moduleId, pageable);
        List<Question> questions = questionPage.getContent();
        if (questions.isEmpty()) {
            return Page.empty(pageable);
        }

        // --- 4. Bulk fetch votes for all questions on the page ---
        List<Integer> questionIds = questions.stream().map(Question::getQuestionId).collect(Collectors.toList());
        List<QuestionVote> votes = questionVoteRepository.findByQuestionQuestionIdIn(questionIds);

        Map<Integer, Integer> scores = votes.stream().collect(Collectors.groupingBy(
                vote -> vote.getQuestion().getQuestionId(),
                Collectors.summingInt(vote -> (int) vote.getValue())
        ));
        Map<Integer, Integer> currentUserVotes = votes.stream()
                .filter(vote -> vote.getUser().getUserId().equals(currentUser.getId()))
                .collect(Collectors.toMap(vote -> vote.getQuestion().getQuestionId(), vote -> (int) vote.getValue()));

        return questionPage.map(question -> QAMapper.toQuestionResponseDto(question, currentUser,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), scores, currentUserVotes));
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "questions", key = "#questionId")
    public QuestionResponseDto getQuestionById(Integer questionId, UserDetailsImpl currentUser) {
        // --- 5. This is the fully optimized implementation ---
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        List<Integer> questionIds = List.of(questionId);
        List<Integer> answerIds = question.getAnswers().stream().map(Answer::getAnswerId).collect(Collectors.toList());
        List<Integer> commentIds = question.getAnswers().stream()
                .flatMap(answer -> answer.getComments().stream())
                .flatMap(comment -> flattenComments(comment).stream())
                .map(Comment::getCommentId)
                .collect(Collectors.toList());

        List<QuestionVote> questionVotes = questionIds.isEmpty() ? Collections.emptyList() : questionVoteRepository.findByQuestionQuestionIdIn(questionIds);
        List<AnswerVote> answerVotes = answerIds.isEmpty() ? Collections.emptyList() : answerVoteRepository.findByAnswerAnswerIdIn(answerIds);
        List<CommentVote> commentVotes = commentIds.isEmpty() ? Collections.emptyList() : commentVoteRepository.findByCommentCommentIdIn(commentIds);

        Map<Integer, Integer> scores = Stream.of(questionVotes, answerVotes, commentVotes)
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(Vote::getPostId, Collectors.summingInt(vote -> (int) vote.getValue())));

        Map<Integer, Integer> currentUserVotes = Stream.of(questionVotes, answerVotes, commentVotes)
                .flatMap(List::stream)
                .filter(vote -> currentUser != null && vote.getUser().getUserId().equals(currentUser.getId()))
                .collect(Collectors.toMap(Vote::getPostId, vote -> (int) vote.getValue(), (v1, v2) -> v1));
        
        Map<Integer, List<Attachment>> questionAttachments = findAttachments("QUESTION", questionIds);
        Map<Integer, List<Attachment>> answerAttachments = findAttachments("ANSWER", answerIds);
        Map<Integer, List<Attachment>> commentAttachments = findAttachments("COMMENT", commentIds);

        return QAMapper.toQuestionResponseDto(question, currentUser, questionAttachments, answerAttachments, commentAttachments, scores, currentUserVotes);
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