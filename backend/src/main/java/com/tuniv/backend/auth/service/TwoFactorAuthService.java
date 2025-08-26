package com.tuniv.backend.auth.service; // Or your correct package

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {

    public String generateNewSecret() {
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        return secretGenerator.generate();
    }

    @SneakyThrows
    public String generateQrCodeImageUri(String secret, String email) {
        QrData data = new QrData.Builder()
            .label(email)
            .secret(secret)
            .issuer("TunivApp")
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();

        ZxingPngQrGenerator qrGenerator = new ZxingPngQrGenerator();
        byte[] imageData = qrGenerator.generate(data);
        return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
    }

    /**
     * A more robust and transparent validation method with detailed logging.
     * Instead of asking "is this code valid?", we generate all possible valid
     * codes for the time window and see if the user's code is in that set.
     */
    public boolean isOtpValid(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);

        // The number of 30-second periods before and after the current time to allow.
        // A value of 2 allows for a ~2.5 minute window (current + 2 before + 2 after).
        int discrepancy = 2;

        System.out.println("--- ROBUST 2FA VALIDATION ---");
        System.out.println("Server Time (UTC): " + Instant.now());
        System.out.println("User's Code: " + code);
        
        try {
            long currentBucket = Math.floorDiv(timeProvider.getTime(), 30);
            System.out.println("Current Time Bucket: " + currentBucket);

            for (int i = -discrepancy; i <= discrepancy; i++) {
                long bucket = currentBucket + i;
                String generatedCode = codeGenerator.generate(secret, bucket);
                System.out.println("Checking bucket " + bucket + "... Generated Code: " + generatedCode);
                if (generatedCode.equals(code)) {
                    System.out.println(">>> MATCH FOUND! <<<");
                    System.out.println("---------------------------");
                    return true;
                }
            }
        } catch (CodeGenerationException e) {
            System.err.println("Error during code generation: " + e.getMessage());
            return false;
        }

        System.out.println(">>> NO MATCH FOUND. Validation failed. <<<");
        System.out.println("---------------------------");
        return false;
    }
}