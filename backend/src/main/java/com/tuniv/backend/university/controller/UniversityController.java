package com.tuniv.backend.university.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.university.dto.UniversityDto;
import com.tuniv.backend.university.service.UniversityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/universities")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    @GetMapping
    public ResponseEntity<List<UniversityDto>> getAllUniversities(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        return ResponseEntity.ok(universityService.getAllUniversities(currentUser));
    }

    // --- The /modules endpoint has been moved to ModuleController ---

    @PostMapping("/{universityId}/members")
    public ResponseEntity<?> joinUniversity(@PathVariable Integer universityId, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        universityService.joinUniversity(universityId, currentUser);
        return ResponseEntity.ok(Map.of("message", "Successfully joined university."));
    }
    
    @DeleteMapping("/{universityId}/members")
    public ResponseEntity<?> unjoinUniversity(@PathVariable Integer universityId, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        universityService.unjoinUniversity(universityId, currentUser);
        return ResponseEntity.ok(Map.of("message", "Successfully unjoined university."));
    }
}