package com.tuniv.backend.university.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.dto.ModuleDetailDto;
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.mapper.UniversityMapper;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.university.repository.UniversityMembershipRepository;
import com.tuniv.backend.university.repository.UniversityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final UniversityRepository universityRepository;
    private final UniversityMembershipRepository membershipRepository;
    private final TopicRepository topicRepository; // ✅ ADDED: For topic counts

    @Transactional(readOnly = true)
    public ModuleDetailDto getModuleDetails(Integer moduleId, UserDetailsImpl currentUser) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));
        
        University parentUniversity = module.getUniversity();

        boolean isMember = membershipRepository.existsByUser_UserIdAndUniversity_UniversityId(
            currentUser.getId(), 
            parentUniversity.getUniversityId()
        );
        
        return UniversityMapper.toModuleDetailDto(module, isMember);
    }

    @Transactional(readOnly = true)
    public Page<ModuleDto> getModulesByUniversity(Integer universityId, Pageable pageable) {
        if (!universityRepository.existsById(universityId)) {
            throw new ResourceNotFoundException("University not found with id: " + universityId);
        }
        
        Page<Module> modulePage = moduleRepository.findByUniversityUniversityId(universityId, pageable);
        return modulePage.map(UniversityMapper::toModuleDto);
    }

    @Transactional(readOnly = true)
    public Page<ModuleDto> getAllModules(Pageable pageable) {
        return moduleRepository.findAll(pageable)
                               .map(UniversityMapper::toModuleDto);
    }

    @Transactional(readOnly = true)
    public List<ModuleDto> getAllModulesByUniversity(Integer universityId) {
        if (!universityRepository.existsById(universityId)) {
            throw new ResourceNotFoundException("University not found with id: " + universityId);
        }
        
        return moduleRepository.findByUniversityUniversityId(universityId).stream()
               .map(UniversityMapper::toModuleDto)
               .collect(Collectors.toList());
    }

    // ✅ NEW: Get topics by module (replaces getQuestionsByModule)
    @Transactional(readOnly = true)
    public Page<TopicSummaryDto> getTopicsByModule(Integer moduleId, Pageable pageable, UserDetailsImpl currentUser) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new ResourceNotFoundException("Module not found with id: " + moduleId);
        }
        
        Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        return topicRepository.findTopicSummariesByModuleId(moduleId, currentUserId, pageable);
    }
}