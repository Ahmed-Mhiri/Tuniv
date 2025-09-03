package com.tuniv.backend.qa.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.model.QuestionVote;
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
    private final QuestionVoteRepository questionVoteRepository; // <-- 1. Inject the repository for votes

    @Transactional(readOnly = true)
    public Page<QuestionResponseDto> getPersonalizedFeed(UserDetailsImpl currentUser, Pageable pageable) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Integer> memberModuleIds = user.getMemberships().stream()
                .flatMap(membership -> membership.getUniversity().getModules().stream())
                .map(Module::getModuleId)
                .collect(Collectors.toList());

        if (memberModuleIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<Question> questionPage = questionRepository.findByModule_ModuleIdIn(memberModuleIds, pageable);
        
        // <-- 2. Get the list of questions from the current page
        List<Question> questions = questionPage.getContent();
        if (questions.isEmpty()) {
            return Page.empty(pageable);
        }

        // <-- 3. Collect all question IDs for bulk fetching
        List<Integer> questionIds = questions.stream().map(Question::getQuestionId).collect(Collectors.toList());

        // <-- 4. Fetch all votes for these questions in a single query
        List<QuestionVote> votes = questionVoteRepository.findByQuestionQuestionIdIn(questionIds);

        // <-- 5. Process votes into maps for scores and the current user's vote
        Map<Integer, Integer> scores = votes.stream()
            .collect(Collectors.groupingBy(
                vote -> vote.getQuestion().getQuestionId(),
                Collectors.summingInt(vote -> (int) vote.getValue())
            ));

        Map<Integer, Integer> currentUserVotes = votes.stream()
            .filter(vote -> vote.getUser().getUserId().equals(currentUser.getId()))
            .collect(Collectors.toMap(
                vote -> vote.getQuestion().getQuestionId(),
                vote -> (int) vote.getValue()
            ));

        // <-- 6. Call the updated mapper with the new maps
        return questionPage.map(question -> QAMapper.toQuestionResponseDto(
                question,
                currentUser,
                Collections.emptyMap(), // Assuming attachments are handled elsewhere for the feed
                Collections.emptyMap(),
                Collections.emptyMap(),
                scores,
                currentUserVotes
        ));
    }
}