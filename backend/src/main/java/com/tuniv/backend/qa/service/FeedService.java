package com.tuniv.backend.qa.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.mapper.QAMapper;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.repository.QuestionRepository;
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

    @Transactional(readOnly = true)
    public Page<QuestionResponseDto> getPersonalizedFeed(UserDetailsImpl currentUser, Pageable pageable) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Step 1: Get all module IDs from the universities the user is a member of.
        List<Integer> memberModuleIds = user.getMemberships().stream()
                .flatMap(membership -> membership.getUniversity().getModules().stream())
                .map(Module::getModuleId) // This line will now work correctly
                .collect(Collectors.toList());

        // If the user has no memberships, return an empty page.
        if (memberModuleIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // Step 2: Fetch a paginated list of questions from those modules.
        Page<Question> questionPage = questionRepository.findByModule_ModuleIdIn(memberModuleIds, pageable);

        // Step 3: Map the Question entities to DTOs for the response.
        return questionPage.map(question -> QAMapper.toQuestionResponseDto(
                question,
                currentUser,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap()
        ));
    }
}