package com.tuniv.backend.auth.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

import com.tuniv.backend.shared.exception.auth.TwoFactorSetupException;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthService {

    @Value("${app.2fa.issuer-name:TunivApp}")
    private String issuerName;

    @Value("${app.2fa.time-discrepancy:2}")
    private int timeDiscrepancy;

    @Value("${app.2fa.code-length:6}")
    private int codeLength;

    @Value("${app.2fa.time-period:30}")
    private int timePeriod;

    /**
     * Generates a new secret key for 2FA setup
     */
    public String generateNewSecret() {
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();
        log.debug("Generated new 2FA secret (length: {})", secret.length());
        return secret;
    }

    /**
     * Generates a QR code image URI for 2FA setup
     */
    public String generateQrCodeImageUri(String secret, String email) {
        log.debug("Generating QR code for user: {}", email);
        
        QrData data = new QrData.Builder()
            .label(email)
            .secret(secret)
            .issuer(issuerName) // ✅ Now configurable
            .algorithm(HashingAlgorithm.SHA1)
            .digits(codeLength)
            .period(timePeriod)
            .build();

        try {
            ZxingPngQrGenerator qrGenerator = new ZxingPngQrGenerator();
            byte[] imageData = qrGenerator.generate(data);
            String qrCodeUri = getDataUriForImage(imageData, qrGenerator.getImageMimeType());
            
            log.debug("Successfully generated QR code for user: {}", email);
            return qrCodeUri;
            
        } catch (Exception e) {
            log.error("Failed to generate QR code for user: {}", email, e);
            throw new TwoFactorSetupException("Failed to generate QR code for 2FA setup", e);
        }
    }

    /**
     * Robust OTP validation with configurable time discrepancy window
     * Generates all possible valid codes for the time window and checks for matches
     */
    public boolean isOtpValid(String secret, String code) {
        if (secret == null || secret.trim().isEmpty()) {
            log.warn("Attempted 2FA validation with null or empty secret");
            return false;
        }
        
        if (code == null || code.trim().isEmpty()) {
            log.warn("Attempted 2FA validation with null or empty code");
            return false;
        }

        String normalizedCode = code.trim();
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, codeLength);

        log.debug("--- 2FA VALIDATION STARTED ---");
        log.debug("Server Time (UTC): {}", Instant.now());
        log.debug("Code length: {}, Time discrepancy: {} periods", codeLength, timeDiscrepancy);
        
        try {
            long currentBucket = Math.floorDiv(timeProvider.getTime(), timePeriod);
            log.debug("Current Time Bucket: {}", currentBucket);

            // Check current and adjacent time buckets to account for clock drift
            for (int i = -timeDiscrepancy; i <= timeDiscrepancy; i++) {
                long bucket = currentBucket + i;
                String generatedCode = codeGenerator.generate(secret, bucket);
                
                log.debug("Checking bucket {} (offset: {}) - Generated: {}", bucket, i, generatedCode);
                
                if (generatedCode.equals(normalizedCode)) {
                    log.info("2FA validation SUCCESS for time bucket {} (offset: {})", bucket, i);
                    log.debug("--- 2FA VALIDATION COMPLETED: SUCCESS ---");
                    return true;
                }
            }

            log.warn("2FA validation FAILED - No match found for code: {} (masked)", maskCode(normalizedCode));
            log.debug("--- 2FA VALIDATION COMPLETED: FAILED ---");
            return false;
            
        } catch (CodeGenerationException e) {
            log.error("Error during 2FA code generation for secret: {} (masked)", maskSecret(secret), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during 2FA validation", e);
            return false;
        }
    }

    /**
     * Alternative validation method that returns detailed result for debugging
     */
    public ValidationResult validateOtpWithDetails(String secret, String code) {
        if (secret == null || secret.trim().isEmpty()) {
            return ValidationResult.error("Secret is null or empty");
        }
        
        if (code == null || code.trim().isEmpty()) {
            return ValidationResult.error("Code is null or empty");
        }

        String normalizedCode = code.trim();
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, codeLength);

        try {
            long currentBucket = Math.floorDiv(timeProvider.getTime(), timePeriod);
            long currentTime = timeProvider.getTime();

            for (int i = -timeDiscrepancy; i <= timeDiscrepancy; i++) {
                long bucket = currentBucket + i;
                String generatedCode = codeGenerator.generate(secret, bucket);
                
                if (generatedCode.equals(normalizedCode)) {
                    return ValidationResult.success(bucket, i, currentTime);
                }
            }

            return ValidationResult.noMatch(currentBucket, currentTime);
            
        } catch (CodeGenerationException e) {
            return ValidationResult.error("Code generation failed: " + e.getMessage());
        }
    }

    /**
     * Utility method to mask secret for logging (security best practice)
     */
    private String maskSecret(String secret) {
        if (secret == null || secret.length() <= 8) {
            return "***";
        }
        return secret.substring(0, 4) + "..." + secret.substring(secret.length() - 4);
    }

    /**
     * Utility method to mask code for logging (security best practice)
     */
    private String maskCode(String code) {
        if (code == null || code.length() <= 2) {
            return "***";
        }
        return code.charAt(0) + "***" + code.charAt(code.length() - 1);
    }

    /**
     * Detailed validation result for debugging and monitoring
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final Long matchedBucket;
        private final Integer timeOffset;
        private final Long serverTime;

        private ValidationResult(boolean valid, String message, Long matchedBucket, Integer timeOffset, Long serverTime) {
            this.valid = valid;
            this.message = message;
            this.matchedBucket = matchedBucket;
            this.timeOffset = timeOffset;
            this.serverTime = serverTime;
        }

        public static ValidationResult success(long bucket, int offset, long serverTime) {
            return new ValidationResult(true, 
                String.format("OTP validated successfully (bucket: %d, offset: %d)", bucket, offset),
                bucket, offset, serverTime);
        }

        public static ValidationResult noMatch(long currentBucket, long serverTime) {
            return new ValidationResult(false,
                String.format("No OTP match found (current bucket: %d)", currentBucket),
                null, null, serverTime);
        }

        public static ValidationResult error(String errorMessage) {
            return new ValidationResult(false, errorMessage, null, null, null);
        }

        // Getters
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public Long getMatchedBucket() { return matchedBucket; }
        public Integer getTimeOffset() { return timeOffset; }
        public Long getServerTime() { return serverTime; }
    }
}