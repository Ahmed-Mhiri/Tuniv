package com.tuniv.backend.user.service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.service.AuthEmailService;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.university.model.UniversityRole;
import com.tuniv.backend.university.model.VerificationStatus;
import com.tuniv.backend.university.repository.UniversityMembershipRepository;
import com.tuniv.backend.university.repository.UniversityRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException.BadRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service responsible for handling the university verification process for users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final UniversityMembershipRepository membershipRepository;
    private final AuthEmailService authEmailService;
    
    // ✅ INJECT UserService to delegate primary university logic
    private final UserService userService;

    /**
     * Initiates the verification process for a user with a given university email.
     * It creates a pending membership record and sends a verification email.
     *
     * @param universityEmail The university-affiliated email address to verify.
     * @param currentUser The principal of the currently logged-in user.
     */
    @Transactional
    public void initiateUniversityVerification(String universityEmail, UserDetailsImpl currentUser) throws BadRequest {
        String domain = getDomainFromEmail(universityEmail);
        log.info("Initiating verification for user {} with email domain '{}'", currentUser.getUsername(), domain);

        University university = universityRepository.findByEmailDomain(domain)
            .orElseThrow(() -> new ResourceNotFoundException("No university found for the domain: " + domain));

        User user = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // Prevent re-initiating verification if the user is already fully verified at this university.
        boolean isAlreadyVerified = membershipRepository.existsByUser_UserIdAndUniversity_UniversityIdAndStatus(
            user.getUserId(), university.getUniversityId(), VerificationStatus.VERIFIED);

        if (isAlreadyVerified) {
            log.warn("User {} is already verified at university '{}'. Aborting verification initiation.", user.getUsername(), university.getName());
            throw new IllegalArgumentException("You are already verified at this university.");
        }

        // Find an existing membership (e.g., a previous pending or rejected one) or create a new one.
        UniversityMembership membership = membershipRepository
            .findByUser_UserIdAndUniversity_UniversityId(user.getUserId(), university.getUniversityId())
            .orElseGet(() -> new UniversityMembership(user, university, UniversityRole.STUDENT));

        String token = UUID.randomUUID().toString();
        membership.setVerificationToken(token);
        membership.setVerifiedEmail(universityEmail);
        membership.setStatus(VerificationStatus.PENDING);
        membership.setVerificationTokenExpiry(Instant.now().plus(24, ChronoUnit.HOURS));

        membershipRepository.save(membership);
        log.info("Saved pending membership for user {} at university '{}'. Sending verification email.", user.getUsername(), university.getName());

        authEmailService.sendUniversityVerificationEmail(universityEmail, token);
    }

    /**
     * Completes the verification process using a provided token.
     * On success, it marks the membership as verified and may set it as the user's primary university.
     *
     * @param token The verification token from the email link.
     */
    @Transactional
    public void completeUniversityVerification(String token) {
        log.info("Attempting to complete university verification with a token.");
        UniversityMembership membership = membershipRepository.findByVerificationToken(token)
            .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired verification token."));

        // Check if the token has expired
        if (membership.getVerificationTokenExpiry().isBefore(Instant.now())) {
            log.warn("Verification token has expired for user {}. Deleting pending membership.", membership.getUser().getUsername());
            membershipRepository.delete(membership);
            throw new IllegalArgumentException("Verification token has expired.");
        }

        // Mark the membership as verified and clean up token data.
        membership.setStatus(VerificationStatus.VERIFIED);
        membership.setVerificationDate(Instant.now());
        membership.setVerificationToken(null);
        membership.setVerificationTokenExpiry(null);

        // Save the verified status first to ensure the count is correct in the next step.
        UniversityMembership verifiedMembership = membershipRepository.saveAndFlush(membership);
        User user = verifiedMembership.getUser();

        // Check the total number of verified memberships for this user.
        long totalVerifiedCount = membershipRepository.countByUser_UserIdAndStatus(user.getUserId(), VerificationStatus.VERIFIED);

        // If this is the user's VERY FIRST verified university, automatically set it as primary.
        if (totalVerifiedCount == 1) {
            log.info("This is the first verified university for user {}. Setting '{}' as primary.", user.getUsername(), verifiedMembership.getUniversity().getName());
            
            // ✅ DELEGATE to UserService to handle the logic consistently and safely.
            userService.setPrimaryUniversity(user.getUserId(), verifiedMembership.getUniversity().getUniversityId());
        }
    }

    /**
     * Extracts the domain from an email address.
     * @param email The email address.
     * @return The domain part of the email.
     */
    private String getDomainFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        return email.substring(email.indexOf("@") + 1);
    }
}