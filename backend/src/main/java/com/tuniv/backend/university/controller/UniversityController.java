package com.tuniv.backend.university.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.dto.UniversityDto;
import com.tuniv.backend.university.service.ModuleService;
import com.tuniv.backend.university.service.UniversityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/universities")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;
    private final ModuleService moduleService; // Kept to serve the modules sub-resource

    // --- Primary University Endpoints ---

    /**
     * GET /api/v1/universities
     * Get a paginated list of all universities, with an optional search term.
     */
    @GetMapping
    public ResponseEntity<Page<UniversityDto>> getAllUniversities(
            @RequestParam(required = false) String search,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<UniversityDto> universityPage = universityService.getAllUniversities(search, pageable, currentUser);
        return ResponseEntity.ok(universityPage);
    }

    /**
     * GET /api/v1/universities/top
     * Get a list of the most popular universities by member count.
     */
    @GetMapping("/top")
    public ResponseEntity<List<UniversityDto>> getTopUniversities(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(universityService.getTopUniversities(currentUser));
    }

    /**
     * GET /api/v1/universities/joined
     * Get a list of universities the current user is a member of.
     */
    @GetMapping("/joined")
    public ResponseEntity<List<UniversityDto>> getJoinedUniversities(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(universityService.getJoinedUniversities(currentUser));
    }

    // --- University Membership Endpoints (Sub-resource: members) ---

    /**
     * POST /api/v1/universities/{universityId}/members
     * Allows the current user to join a specific university.
     */
    @PostMapping("/{universityId}/members")
    public ResponseEntity<?> joinUniversity(
            @PathVariable Integer universityId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        universityService.joinUniversity(universityId, currentUser);
        return ResponseEntity.ok(Map.of("message", "Successfully joined university."));
    }

    /**
     * DELETE /api/v1/universities/{universityId}/members
     * Allows the current user to leave a specific university.
     */
    @DeleteMapping("/{universityId}/members")
    public ResponseEntity<?> unjoinUniversity(
            @PathVariable Integer universityId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        universityService.unjoinUniversity(universityId, currentUser);
        return ResponseEntity.ok(Map.of("message", "Successfully unjoined university."));
    }

    // --- University Modules Endpoints (Sub-resource: modules) ---

    /**
     * GET /api/v1/universities/{universityId}/modules
     * Get a paginated list of modules for a specific university.
     */
    @GetMapping("/{universityId}/modules")
    public ResponseEntity<Page<ModuleDto>> getModulesByUniversity(
            @PathVariable Integer universityId,
            Pageable pageable) {
        return ResponseEntity.ok(moduleService.getModulesByUniversity(universityId, pageable));
    }

    /**
     * GET /api/v1/universities/{universityId}/modules/all
     * Get a complete, non-paginated list of all modules for a specific university.
     */
    @GetMapping("/{universityId}/modules/all")
    public ResponseEntity<List<ModuleDto>> getAllModulesByUniversity(
            @PathVariable Integer universityId) {
        return ResponseEntity.ok(moduleService.getAllModulesByUniversity(universityId));
    }

    // --- ✅ NEW: University Topics Endpoint (Sub-resource: topics) ---

    /**
     * GET /api/v1/universities/{universityId}/topics
     * Get a paginated list of all topics associated with a university's modules.
     */
    @GetMapping("/{universityId}/topics")
    public ResponseEntity<Page<TopicSummaryDto>> getTopicsByUniversity(
            @PathVariable Integer universityId,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<TopicSummaryDto> topics = universityService.getTopicsByUniversity(universityId, pageable, currentUser);
        return ResponseEntity.ok(topics);
    }
}