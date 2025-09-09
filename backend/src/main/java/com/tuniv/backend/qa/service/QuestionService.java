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
    private final VoteRepository voteRepository; // ✨ INJECT THE NEW REPOSITORY (replaces CustomVoteRepository)



    @Transactional(readOnly = true)
    @Cacheable(value = "questions", key = "#questionId")
    public QuestionResponseDto getQuestionById(Integer questionId, UserDetailsImpl currentUser) {
        Question question = questionRepository.findWithAuthorAndModuleById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        List<Answer> answers = answerRepository.findAllByQuestionIdsWithDetails(Collections.singletonList(questionId));
        List<Integer> answerIds = answers.stream().map(Answer::getId).toList();

        List<Comment> topLevelComments = answerIds.isEmpty() ? Collections.emptyList() :
                commentRepository.findTopLevelByAnswerIdsWithDetails(answerIds);
        Map<Integer, List<Comment>> commentsByAnswerId = topLevelComments.stream()
                .collect(Collectors.groupingBy(comment -> comment.getAnswer().getId()));

        Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (currentUser != null) {
            // ✅ OPTIMIZATION: Combine all post IDs and fetch votes in one go.
            List<Integer> commentIds = topLevelComments.stream()
                    .flatMap(comment -> flattenComments(comment).stream())
                    .map(Comment::getId)
                    .toList();
            
            // Combine all IDs into a single list
            List<Integer> allPostIds = Stream.concat(
                Stream.of(questionId),
                Stream.concat(answerIds.stream(), commentIds.stream())
            ).toList();

            if (!allPostIds.isEmpty()) {
                List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), allPostIds);
                currentUserVotes = votes.stream()
                    .collect(Collectors.toMap(VoteInfo::postId, VoteInfo::value));
            }
        }

        return QAMapper.buildQuestionResponseDto(question, answers, commentsByAnswerId, currentUser, currentUserVotes);
    }


    @Transactional(readOnly = true)
    public Page<QuestionSummaryDto> getQuestionsByModule(Integer moduleId, Pageable pageable, UserDetailsImpl currentUser) {
        Page<QuestionSummaryDto> summaryPage = questionRepository.findQuestionSummariesByModuleId(moduleId, pageable);
        if (currentUser != null && !summaryPage.isEmpty()) {
            List<Integer> questionIds = summaryPage.stream().map(QuestionSummaryDto::id).toList();
            // ✅ UPDATED: Use the new repository for a single, efficient query
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
        
        // ✅ OPTIMIZATION: Manually build the DTO to avoid slow, unnecessary database calls.
        return QAMapper.buildQuestionResponseDto(
            savedQuestion, 
            Collections.emptyList(), // New question has no answers
            Collections.emptyMap(),  // New question has no comments
            currentUser, 
            Collections.emptyMap()   // New question has no votes yet
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
        
        return getQuestionById(questionId, currentUser);
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#questionId")
    public void deleteQuestion(Integer questionId, UserDetailsImpl currentUser) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        postAuthorizationService.checkOwnership(question, currentUser);
        questionRepository.delete(question);
    }

    private List<Comment> flattenComments(Comment comment) {
        List<Comment> list = new ArrayList<>();
        list.add(comment);
        if (comment.getChildren() != null && !comment.getChildren().isEmpty()) {
            comment.getChildren().forEach(child -> list.addAll(flattenComments(child)));
        }
        return list;
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
}