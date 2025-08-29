package com.tuniv.backend.university.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.mapper.UniversityMapper;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.university.repository.UniversityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final UniversityRepository universityRepository;

    /**
     * Fetches all modules for a specific university.
     */
    @Transactional(readOnly = true)
    public List<ModuleDto> getModulesByUniversity(Integer universityId) {
        if (!universityRepository.existsById(universityId)) {
            throw new ResourceNotFoundException("University not found with id: " + universityId);
        }
        return moduleRepository.findByUniversityUniversityId(universityId).stream()
                .map(UniversityMapper::toModuleDto)
                .collect(Collectors.toList());
    }

    /**
     * Fetches a flat list of all modules in the system.
     * Useful for dropdowns, like on the "Ask Question" page.
     */
    @Transactional(readOnly = true)
    public List<ModuleDto> getAllModules() {
        return moduleRepository.findAll().stream()
                .map(UniversityMapper::toModuleDto)
                .collect(Collectors.toList());
    }
}
