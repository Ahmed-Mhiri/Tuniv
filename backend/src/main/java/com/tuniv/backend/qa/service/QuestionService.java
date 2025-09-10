package com.tuniv.backend.qa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable; // Import all models
import org.springframework.stereotype.Service; // Import all repositories
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.auth.service.PostAuthorizationService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerCreateRequest;
import com.tuniv.backend.qa.dto.QuestionCreateRequest;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.dto.QuestionSummaryDto;
import com.tuniv.backend.qa.dto.QuestionUpdateRequest;
import com.tuniv.backend.qa.dto.VoteInfo;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.CommentRepository;
import com.tuniv.backend.qa.repository.QuestionRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
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
    private final CommentRepository commentRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final AttachmentService attachmentService;
    private final PostAuthorizationService postAuthorizationService;
    private final VoteRepository voteRepository;

    /**
     * ✅ REFACTORED AND HIGHLY OPTIMIZED
     * This method now fetches the entire Question data tree in a single query using an Entity Graph,
     * followed by a second query to fetch all user-specific votes.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "questions", key = "#questionId")
    public QuestionResponseDto getQuestionById(Integer questionId, UserDetailsImpl currentUser) {
        // STEP 1: Fetch the question and its entire tree in ONE database call. 🚀
        Question question = questionRepository.findFullTreeById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        // STEP 2: Collect all Post IDs from the fully-loaded question object.
        Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (currentUser != null) {
            List<Integer> allPostIds = new ArrayList<>();
            allPostIds.add(question.getId());
            question.getAnswers().forEach(answer -> {
                allPostIds.add(answer.getId());
                // Flatten the comment tree to get all comment IDs
                answer.getComments().stream()
                    .flatMap(this::flattenCommentsStream)
                    .forEach(comment -> allPostIds.add(comment.getId()));
            });

            // STEP 3: Fetch all votes for the current user in ONE database call.
            if (!allPostIds.isEmpty()) {
                List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), allPostIds);
                currentUserVotes = votes.stream()
                    .collect(Collectors.toMap(VoteInfo::postId, VoteInfo::value));
            }
        }

        // STEP 4: Map the single, fully-populated entity to a DTO.
        // NOTE: Ensure your QAMapper is updated to handle this simpler call signature.
        return QAMapper.buildQuestionResponseDto(question, currentUser, currentUserVotes);
    }

    @Transactional(readOnly = true)
    public Page<QuestionSummaryDto> getQuestionsByModule(Integer moduleId, Pageable pageable, UserDetailsImpl currentUser) {
        Page<QuestionSummaryDto> summaryPage = questionRepository.findQuestionSummariesByModuleId(moduleId, pageable);
        if (currentUser != null && !summaryPage.isEmpty()) {
            List<Integer> questionIds = summaryPage.stream().map(QuestionSummaryDto::id).toList();
            List<VoteInfo> userVotes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), questionIds);
            Map<Integer, Integer> userVoteMap = userVotes.stream()
                    .collect(Collectors.toMap(VoteInfo::postId, VoteInfo::value));

            List<QuestionSummaryDto> updatedSummaries = summaryPage.getContent().stream()
                    .map(summary -> summary.withCurrentUserVote(userVoteMap.getOrDefault(summary.id(), 0))).toList();
            return new PageImpl<>(updatedSummaries, pageable, summaryPage.getTotalElements());
        }
        return summaryPage;
    }

    @Transactional
    public QuestionResponseDto createQuestion(QuestionCreateRequest request, UserDetailsImpl currentUser, List<MultipartFile> files) {
        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
        Module module = moduleRepository.findById(request.moduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Error: Module not found with id: " + request.moduleId()));

        Question question = new Question();
        question.setTitle(request.title());
        question.setBody(request.body());
        question.setModule(module);
        question.setAuthor(author);
        
        Question savedQuestion = questionRepository.save(question);
        attachmentService.saveAttachments(files, savedQuestion);
        
        // This is already well-optimized to avoid re-fetching.
        return QAMapper.buildQuestionResponseDto(
                savedQuestion, 
                Collections.emptyList(),
                Collections.emptyMap(), 
                currentUser, 
                Collections.emptyMap() 
        );
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#questionId")
    public QuestionResponseDto addAnswer(Integer questionId, AnswerCreateRequest request, UserDetailsImpl currentUser, List<MultipartFile> files) {
        User author = userRepository.findById(currentUser.getId()).orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new ResourceNotFoundException("Error: Question not found with id: " + questionId));
        
        Answer answer = new Answer();
        answer.setBody(request.body());
        answer.setQuestion(question);
        answer.setAuthor(author);
        
        Answer savedAnswer = answerRepository.save(answer);
        attachmentService.saveAttachments(files, savedAnswer);

        // This method now calls our highly optimized getQuestionById, making it faster.
        return this.getQuestionById(questionId, currentUser);
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#questionId")
    public QuestionResponseDto updateQuestion(Integer questionId, QuestionUpdateRequest request, List<MultipartFile> newFiles, UserDetailsImpl currentUser) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        postAuthorizationService.checkOwnership(question, currentUser);
        
        question.setTitle(request.title());
        question.setBody(request.body());
        
        if (request.attachmentIdsToDelete() != null && !request.attachmentIdsToDelete().isEmpty()) {
            Set<Attachment> toDelete = question.getAttachments().stream()
                    .filter(att -> request.attachmentIdsToDelete().contains(att.getAttachmentId()))
                    .collect(Collectors.toSet());
            attachmentService.deleteAttachments(toDelete);
            toDelete.forEach(question::removeAttachment);
        }
        
        attachmentService.saveAttachments(newFiles, question);
        questionRepository.save(question);
        
        // This method also calls our highly optimized getQuestionById.
        return getQuestionById(questionId, currentUser);
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#questionId")
    public void deleteQuestion(Integer questionId, UserDetailsImpl currentUser) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        postAuthorizationService.checkOwnership(question, currentUser);
        questionRepository.delete(question);
    }
    
    @Transactional(readOnly = true)
    public Integer findQuestionIdByAnswerId(Integer answerId) {
        return answerRepository.findQuestionIdById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find question for answer id: " + answerId));
    }

    @Transactional(readOnly = true)
    public Integer findQuestionIdByCommentId(Integer commentId) {
        return commentRepository.findQuestionIdById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find question for comment id: " + commentId));
    }

    /**
     * A stream-based helper to recursively flatten a comment and its children.
     * This is used to collect all post IDs for the vote lookup.
     */
    private Stream<Comment> flattenCommentsStream(Comment comment) {
        if (comment.getChildren() == null || comment.getChildren().isEmpty()) {
            return Stream.of(comment);
        }
        // Creates a stream of the parent comment, followed by the flattened stream of all its children.
        return Stream.concat(
            Stream.of(comment),
            comment.getChildren().stream().flatMap(this::flattenCommentsStream)
        );
    }
}