package com.tuniv.backend.university.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.university.dto.ModuleDetailDto;
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.service.ModuleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1") // A common base path
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    /**
     * Endpoint to get all modules for a specific university.
     */
    @GetMapping("/universities/{universityId}/modules")
    public ResponseEntity<List<ModuleDto>> getModulesByUniversity(@PathVariable Integer universityId) {
        return ResponseEntity.ok(moduleService.getModulesByUniversity(universityId));
    }

    /**
     * Endpoint to get a list of all modules.
     */
    @GetMapping("/modules")
    public ResponseEntity<List<ModuleDto>> getAllModules() {
        return ResponseEntity.ok(moduleService.getAllModules());
    }
    @GetMapping("/modules/{moduleId}")
    public ResponseEntity<ModuleDetailDto> getModuleById(
        @PathVariable Integer moduleId,
        @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        return ResponseEntity.ok(moduleService.getModuleDetails(moduleId, currentUser));
    }
}
