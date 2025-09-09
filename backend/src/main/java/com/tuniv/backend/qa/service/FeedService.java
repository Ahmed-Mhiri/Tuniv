package com.tuniv.backend.qa.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.QuestionSummaryDto;
import com.tuniv.backend.qa.dto.VoteInfo;
import com.tuniv.backend.qa.repository.QuestionRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
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
    private final VoteRepository voteRepository; // ✨ INJECT THE CONSOLIDATED REPOSITORY

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

            // ✅ UPDATED: Use the new repository for a single, efficient query
            List<VoteInfo> userVotes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), questionIds);
            Map<Integer, Integer> userVoteMap = userVotes.stream()
                    .collect(Collectors.toMap(VoteInfo::postId, VoteInfo::value));

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
            
            // ✅ UPDATED: Use the new repository for a single, efficient query
            List<VoteInfo> userVotes = voteRepository.findAllVotesForUserByPostIds(currentUser.getId(), questionIds);
            Map<Integer, Integer> userVoteMap = userVotes.stream()
                    .collect(Collectors.toMap(VoteInfo::postId, VoteInfo::value));
            
            List<QuestionSummaryDto> updatedSummaries = summaryPage.getContent().stream()
                    .map(summary -> summary.withCurrentUserVote(userVoteMap.getOrDefault(summary.id(), 0)))
                    .toList();

            return new PageImpl<>(updatedSummaries, pageable, summaryPage.getTotalElements());
        }

        return summaryPage;
    }
}
