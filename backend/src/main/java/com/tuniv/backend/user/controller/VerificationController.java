package com.tuniv.backend.user.controller;

import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.dto.ApiResponse;
import com.tuniv.backend.user.dto.VerificationRequestDto;
import com.tuniv.backend.user.service.VerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    /**
     * Endpoint for the current user to initiate the university verification process.
     * @param requestDto Contains the university email to be verified.
     * @param currentUser The currently authenticated user.
     */
    @PostMapping("/verifications/university")
    public ResponseEntity<ApiResponse> requestUniversityVerification(
            @Valid @RequestBody VerificationRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) throws BadRequestException {
        
        verificationService.initiateUniversityVerification(requestDto.universityEmail(), currentUser);
        
        return ResponseEntity.ok(new ApiResponse(true, "Verification email sent to " + requestDto.universityEmail() + ". Please check your inbox."));
    }

    /**
     * Endpoint to complete the verification process via an email token.
     * This endpoint should be publicly accessible.
     * @param token The verification token from the email link.
     */
    @GetMapping("/auth/verify/university")
    public ResponseEntity<ApiResponse> confirmUniversityVerification(@RequestParam("token") String token) throws BadRequestException {
        verificationService.completeUniversityVerification(token);
        return ResponseEntity.ok(new ApiResponse(true, "University email verified successfully! You now have the 'Verified Student' badge."));
    }
}