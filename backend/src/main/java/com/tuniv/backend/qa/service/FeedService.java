package com.tuniv.backend.qa.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.dto.QuestionSummaryDto;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.AnswerVote;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.CommentVote;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.model.QuestionVote;
import com.tuniv.backend.qa.model.Vote;
import com.tuniv.backend.qa.repository.AnswerRepository;
import com.tuniv.backend.qa.repository.AnswerVoteRepository;
import com.tuniv.backend.qa.repository.CommentRepository;
import com.tuniv.backend.qa.repository.CommentVoteRepository;
import com.tuniv.backend.qa.repository.QuestionRepository;
import com.tuniv.backend.qa.repository.QuestionVoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class FeedService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVoteRepository questionVoteRepository;

    @Transactional(readOnly = true)
    public Page<QuestionSummaryDto> getPersonalizedFeed(UserDetailsImpl currentUser, Pageable pageable) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Integer> memberModuleIds = user.getMemberships().stream()
                .flatMap(membership -> membership.getUniversity().getModules().stream())
                .map(Module::getModuleId)
                .toList();

        if (memberModuleIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<QuestionSummaryDto> summaryPage = questionRepository.findQuestionSummariesByModuleIdIn(memberModuleIds, pageable);

        if (!summaryPage.isEmpty()) {
            List<Integer> questionIds = summaryPage.stream().map(QuestionSummaryDto::id).toList();
            List<QuestionVote> userVotes = questionVoteRepository.findByUserIdAndQuestionIdIn(currentUser.getId(), questionIds);
            Map<Integer, Integer> userVoteMap = userVotes.stream()
                    .collect(Collectors.toMap(v -> v.getQuestion().getId(), v -> (int) v.getValue()));

            List<QuestionSummaryDto> updatedSummaries = summaryPage.getContent().stream()
                    .map(summary -> summary.withCurrentUserVote(userVoteMap.getOrDefault(summary.id(), 0)))
                    .toList();
            
            return new PageImpl<>(updatedSummaries, pageable, summaryPage.getTotalElements());
        }

        return summaryPage;
    }

    @Transactional(readOnly = true)
    public Page<QuestionSummaryDto> getPopularFeed(Pageable pageable, UserDetailsImpl currentUser) {
        Page<QuestionSummaryDto> summaryPage = questionRepository.findPopularQuestionSummaries(pageable);

        if (currentUser != null && !summaryPage.isEmpty()) {
            List<Integer> questionIds = summaryPage.stream().map(QuestionSummaryDto::id).toList();
            List<QuestionVote> userVotes = questionVoteRepository.findByUserIdAndQuestionIdIn(currentUser.getId(), questionIds);
            Map<Integer, Integer> userVoteMap = userVotes.stream()
                    .collect(Collectors.toMap(v -> v.getQuestion().getId(), v -> (int) v.getValue()));
            
            List<QuestionSummaryDto> updatedSummaries = summaryPage.getContent().stream()
                    .map(summary -> summary.withCurrentUserVote(userVoteMap.getOrDefault(summary.id(), 0)))
                    .toList();

            return new PageImpl<>(updatedSummaries, pageable, summaryPage.getTotalElements());
        }

        return summaryPage;
    }
}
