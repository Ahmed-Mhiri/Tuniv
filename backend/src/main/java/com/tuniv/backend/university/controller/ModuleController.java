package com.tuniv.backend.university.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.QuestionSummaryDto;
import com.tuniv.backend.qa.service.QuestionService;
import com.tuniv.backend.university.dto.ModuleDetailDto;
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.service.ModuleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;
    private final QuestionService questionService; // ✅ 2. Inject the QuestionService

    /**
     * GET /api/v1/modules
     * Get a paginated list of all modules.
     */
    @GetMapping
    public ResponseEntity<Page<ModuleDto>> getAllModules(Pageable pageable) {
        return ResponseEntity.ok(moduleService.getAllModules(pageable));
    }

    /**
     * GET /api/v1/modules/{moduleId}
     * Get detailed information for a single module.
     */
    @GetMapping("/{moduleId}")
    public ResponseEntity<ModuleDetailDto> getModuleById(
            @PathVariable Integer moduleId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(moduleService.getModuleDetails(moduleId, currentUser));
    }

    /**
     * ✅ 3. Add this method to handle GET /api/v1/modules/{moduleId}/questions
     * This method resolves the "No static resource found" error.
     */
    @GetMapping("/{moduleId}/questions")
    public ResponseEntity<Page<QuestionSummaryDto>> getQuestionsByModule(
            @PathVariable Integer moduleId,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        Page<QuestionSummaryDto> questionPage = questionService.getQuestionsByModule(moduleId, pageable, currentUser);
        return ResponseEntity.ok(questionPage);
    }
}