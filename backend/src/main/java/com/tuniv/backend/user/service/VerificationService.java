package com.tuniv.backend.user.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.service.AuthEmailService;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.university.model.UserRoleEnum;
import com.tuniv.backend.university.model.VerificationStatus;
import com.tuniv.backend.university.repository.UniversityMembershipRepository;
import com.tuniv.backend.university.repository.UniversityRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final UniversityMembershipRepository membershipRepository;
    private final AuthEmailService authEmailService;

    // --- METHOD TO START THE VERIFICATION ---
    @Transactional
    public void initiateUniversityVerification(String universityEmail, UserDetailsImpl currentUser) throws BadRequestException {
        // 1. Get the domain from the provided email
        String domain = getDomainFromEmail(universityEmail);

        // 2. Find the university associated with this domain
        University university = universityRepository.findByEmailDomain(domain)
                .orElseThrow(() -> new ResourceNotFoundException("No university found for the domain: " + domain));

        // 3. Get the currently logged-in user
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // 4. Check if the user is already verified at this university
        boolean isAlreadyVerified = user.getMemberships().stream()
            .anyMatch(m -> m.getUniversity().equals(university) && m.getStatus() == VerificationStatus.VERIFIED);
        
        if (isAlreadyVerified) {
            throw new BadRequestException("You are already verified at this university.");
        }

        // 5. Create a new membership record (it will be PENDING by default)
        UniversityMembership membership = new UniversityMembership(user, university, UserRoleEnum.STUDENT);
        
        // 6. Generate a secure token
        String token = UUID.randomUUID().toString();
        membership.setVerificationToken(token);
        membership.setVerificationTokenExpiry(OffsetDateTime.now().plusHours(24)); // Token valid for 24 hours

        membershipRepository.save(membership);

        // 7. Send the verification email TO THE UNIVERSITY EMAIL
        authEmailService.sendUniversityVerificationEmail(universityEmail, token);
    }
    
    // --- METHOD TO COMPLETE THE VERIFICATION ---
    @Transactional
    public void completeUniversityVerification(String token) throws BadRequestException {
        // 1. Find the pending verification by the token
        UniversityMembership membership = membershipRepository.findByVerificationToken(token)
            .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired verification token."));

        // 2. Check if the token has expired
        if (membership.getVerificationTokenExpiry().isBefore(OffsetDateTime.now())) {
            membershipRepository.delete(membership); // Clean up expired attempt
            throw new BadRequestException("Verification token has expired.");
        }

        // 3. Success! Update the status and clean up the token
        membership.setStatus(VerificationStatus.VERIFIED);
        membership.setVerificationToken(null);
        membership.setVerificationTokenExpiry(null);

        membershipRepository.save(membership);
    }

    private String getDomainFromEmail(String email) throws BadRequestException {
        if (email == null || !email.contains("@")) {
            throw new BadRequestException("Invalid email format.");
        }
        return email.substring(email.indexOf("@") + 1);
    }
}