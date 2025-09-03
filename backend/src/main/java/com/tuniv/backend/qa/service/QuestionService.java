package com.tuniv.backend.qa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewAnswerEvent;
import com.tuniv.backend.notification.event.NewQuestionInUniversityEvent;
import com.tuniv.backend.qa.dto.AnswerCreateRequest;
import com.tuniv.backend.qa.dto.AnswerResponseDto;
import com.tuniv.backend.qa.dto.QuestionCreateRequest;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.AnswerVote;
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
    // ✅ FIX: AttachmentRepository is no longer needed here.

    // Vote repositories are still needed for efficient bulk fetching
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
        attachmentService.saveAttachments(files, savedQuestion);

        // ✅ FIX: No need to re-fetch or build attachment maps.
        // The 'savedQuestion' object is managed and has its ID.

        eventPublisher.publishEvent(new NewQuestionInUniversityEvent(this, savedQuestion));

        // ✅ FIX: Call the updated, simpler mapper.
        return QAMapper.toQuestionResponseDto(savedQuestion, currentUser,
                Collections.emptyMap(), Collections.emptyMap());
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#questionId")
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

        // ✅ FIX: No need to re-fetch or build attachment maps.

        eventPublisher.publishEvent(new NewAnswerEvent(this, savedAnswer));

        // ✅ FIX: Call the updated, simpler mapper for the answer.
        return QAMapper.toAnswerResponseDto(savedAnswer, currentUser,
                Collections.emptyMap(), Collections.emptyMap());
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponseDto> getQuestionsByModule(Integer moduleId, Pageable pageable, UserDetailsImpl currentUser) {
        Page<Question> questionPage = questionRepository.findByModuleModuleId(moduleId, pageable);
        List<Question> questions = questionPage.getContent();
        if (questions.isEmpty()) {
            return Page.empty(pageable);
        }

        // ✅ FIX: Use getId() from the Post superclass.
        List<Integer> questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());
        List<QuestionVote> votes = questionVoteRepository.findByQuestionIdIn(questionIds);

        Map<Integer, Integer> scores = votes.stream().collect(Collectors.groupingBy(
                vote -> vote.getQuestion().getId(), // ✅ FIX: Use getId()
                Collectors.summingInt(vote -> (int) vote.getValue())
        ));
        Map<Integer, Integer> currentUserVotes = votes.stream()
                .filter(vote -> vote.getUser().getUserId().equals(currentUser.getId()))
                .collect(Collectors.toMap(vote -> vote.getQuestion().getId(), vote -> (int) vote.getValue())); // ✅ FIX: Use getId()

        // ✅ FIX: Call the updated mapper in the page map.
        return questionPage.map(question -> QAMapper.toQuestionResponseDto(question, currentUser, scores, currentUserVotes));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "questions", key = "#questionId")
    public QuestionResponseDto getQuestionById(Integer questionId, UserDetailsImpl currentUser) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        // Efficiently fetch all votes for the question, its answers, and their comments
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

        // ✅ FIX: The attachment maps are gone! The mapper handles it.
        // We simply call the new, simpler mapper.
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

    // ✅ FIX: The findAttachments helper method is no longer needed and has been removed.
}