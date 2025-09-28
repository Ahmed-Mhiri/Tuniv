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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.auth.service.PostAuthorizationService;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.community.repository.CommunityRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewQuestionEvent;
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
import com.tuniv.backend.qa.model.Tag;
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
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final AttachmentService attachmentService;
    private final PostAuthorizationService postAuthorizationService;
    private final VoteRepository voteRepository;
    private final TagService tagService;
    private final ApplicationEventPublisher eventPublisher; // ✅ Make sure this is included


    @Transactional(readOnly = true)
    @Cacheable(value = "questions", key = "#questionId")
    public QuestionResponseDto getQuestionById(Integer questionId, UserDetailsImpl currentUser) {
        Question question = questionRepository.findFullTreeById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        Map<Integer, Integer> currentUserVotes = new HashMap<>();
        if (currentUser != null) {
            List<Integer> allPostIds = new ArrayList<>();
            allPostIds.add(question.getId());
            question.getAnswers().forEach(answer -> {
                allPostIds.add(answer.getId());
                answer.getComments().stream()
                        .flatMap(this::flattenCommentsStream)
                        .forEach(comment -> allPostIds.add(comment.getId()));
            });

            if (!allPostIds.isEmpty()) {
                List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), allPostIds);
                currentUserVotes = votes.stream()
                        .collect(Collectors.toMap(VoteInfo::postId, VoteInfo::value));
            }
        }
        return QAMapper.buildQuestionResponseDto(question, currentUser, currentUserVotes);
    }

    @Transactional(readOnly = true)
    public Page<QuestionSummaryDto> getQuestionsByModule(Integer moduleId, Pageable pageable, UserDetailsImpl currentUser) {
        Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        Page<QuestionSummaryDto> summaryPage = questionRepository.findQuestionSummariesByModuleId(moduleId, currentUserId, pageable);

        if (summaryPage.isEmpty()) {
            return summaryPage;
        }

        return enrichSummariesWithTags(summaryPage, pageable);
    }

    @Transactional
public QuestionResponseDto createQuestion(QuestionCreateRequest request, UserDetailsImpl currentUser, List<MultipartFile> files) {
    // --- Validation: Ensure exactly one container is specified ---
    if ((request.moduleId() == null && request.communityId() == null) || 
        (request.moduleId() != null && request.communityId() != null)) {
        throw new IllegalArgumentException("A question must be posted in exactly one module OR one community.");
    }

    User author = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
    
    Set<Tag> tags = tagService.findOrCreateTags(request.tags());

    Question question = new Question();
    question.setTitle(request.title());
    question.setBody(request.body());
    question.setAuthor(author);
    question.setTags(tags);

    // --- NEW LOGIC: Assign to either Module or Community ---
    if (request.moduleId() != null) {
        Module module = moduleRepository.findById(request.moduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Error: Module not found with id: " + request.moduleId()));
        question.setModule(module);
        
        // ✅ Update module and university question counts
        module.incrementQuestionCount();
        moduleRepository.save(module);
        // University count is automatically updated via module.incrementQuestionCount()
        
    } else { // communityId must be non-null here due to validation above
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Error: Community not found with id: " + request.communityId()));
        question.setCommunity(community);
        
        // ✅ Update community question count
        community.incrementQuestionCount();
        communityRepository.save(community);
    }
    
    Question savedQuestion = questionRepository.save(question);
    attachmentService.saveAttachments(files, savedQuestion);

    // ✅ PUBLISH EVENT FOR NEW QUESTION NOTIFICATIONS
    eventPublisher.publishEvent(new NewQuestionEvent(savedQuestion));
    
    return this.getQuestionById(savedQuestion.getId(), currentUser);
}


    @Transactional
    @CacheEvict(value = "questions", key = "#questionId")
    public QuestionResponseDto updateQuestion(Integer questionId, QuestionUpdateRequest request, List<MultipartFile> newFiles, UserDetailsImpl currentUser) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        postAuthorizationService.checkOwnership(question, currentUser);
        
        // Update only the content fields - container (module/community) remains unchanged
        question.setTitle(request.title());
        question.setBody(request.body());
        
        // Update tags
        Set<Tag> newTags = tagService.findOrCreateTags(request.tags());
        question.getTags().clear();
        question.getTags().addAll(newTags);
        
        // Handle attachment deletions
        if (request.attachmentIdsToDelete() != null && !request.attachmentIdsToDelete().isEmpty()) {
            Set<Attachment> toDelete = question.getAttachments().stream()
                    .filter(att -> request.attachmentIdsToDelete().contains(att.getAttachmentId()))
                    .collect(Collectors.toSet());
            attachmentService.deleteAttachments(toDelete);
            toDelete.forEach(question::removeAttachment);
        }
        
        // Add new attachments
        attachmentService.saveAttachments(newFiles, question);
        
        // Save the updated question
        Question updatedQuestion = questionRepository.save(question);
        
        return getQuestionById(updatedQuestion.getId(), currentUser);
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
        
        question.setAnswerCount(question.getAnswerCount() + 1);

        Answer savedAnswer = answerRepository.save(answer);
        attachmentService.saveAttachments(files, savedAnswer);

        return this.getQuestionById(questionId, currentUser);
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#questionId")
    public void deleteQuestion(Integer questionId, UserDetailsImpl currentUser) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        postAuthorizationService.checkOwnership(question, currentUser);
        
        // ✅ Update counts based on where the question was posted
        if (question.getModule() != null) {
            Module module = question.getModule();
            module.decrementQuestionCount();
            moduleRepository.save(module);
            // University count is automatically updated via module.decrementQuestionCount()
            
        } else if (question.getCommunity() != null) {
            Community community = question.getCommunity();
            community.decrementQuestionCount();
            communityRepository.save(community);
        }
        
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

    @Transactional(readOnly = true)
    public Page<QuestionSummaryDto> getQuestionsByCommunity(Integer communityId, Pageable pageable, UserDetailsImpl currentUser) {
        // 1. Check if the community exists to fail fast
        if (!communityRepository.existsById(communityId)) {
            throw new ResourceNotFoundException("Community not found with id: " + communityId);
        }

        // 2. Fetch the paginated summary data from the repository
        Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        Page<QuestionSummaryDto> summaryPage = questionRepository.findQuestionSummariesByCommunityId(communityId, currentUserId, pageable);

        // 3. Reuse the existing helper method to enrich the results with tags
        return enrichSummariesWithTags(summaryPage, pageable);
    }

    private Page<QuestionSummaryDto> enrichSummariesWithTags(Page<QuestionSummaryDto> summaryPage, Pageable pageable) {
        if (summaryPage.isEmpty()) {
            return summaryPage;
        }

        List<Integer> questionIds = summaryPage.getContent().stream()
                .map(QuestionSummaryDto::id)
                .toList();

        List<Question> questionsWithTags = questionRepository.findWithTagsByIdIn(questionIds);

        Map<Integer, List<String>> tagsMap = questionsWithTags.stream()
                .collect(Collectors.toMap(
                    Question::getId,
                    q -> q.getTags().stream().map(Tag::getName).toList()
                ));
        
        List<QuestionSummaryDto> enrichedSummaries = summaryPage.getContent().stream()
            .map(summary -> summary.withTags(tagsMap.getOrDefault(summary.id(), Collections.emptyList())))
            .toList();

        return new PageImpl<>(enrichedSummaries, pageable, summaryPage.getTotalElements());
    }

    private Stream<Comment> flattenCommentsStream(Comment comment) {
        if (comment.getChildren() == null || comment.getChildren().isEmpty()) {
            return Stream.of(comment);
        }
        return Stream.concat(
            Stream.of(comment),
            comment.getChildren().stream().flatMap(this::flattenCommentsStream)
        );
    }
}