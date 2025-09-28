package com.tuniv.backend.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper; // <-- Import
import org.springframework.scheduling.annotation.Async; // <-- Import
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend.url}") // Inject the frontend URL from application.properties
    private String frontendUrl;

    
    @Async
    public void sendPasswordResetEmail(String to, String token) {
        try {
            // Construct the reset URL using the injected frontendUrl
            String resetUrl = frontendUrl + "/reset-password?token=" + token;
            String subject = "Password Reset Request";

            Context context = new Context();
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("subject", subject);

            String htmlContent = templateEngine.process("password-reset-template", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            // Add the same logo here
            helper.addInline("logoImage", new ClassPathResource("static/images/logo.svg"));

            mailSender.send(mimeMessage);
            System.out.println("HTML Password reset email sent to " + to);

        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
     @Async
    public void sendVerificationEmail(String to, String token) {
        try {
            String verificationUrl = frontendUrl + "/verify-email?token=" + token;
            String subject = "Please Verify Your Email Address";

            // 1. Create Thymeleaf context
            Context context = new Context();
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("subject", subject);

            // 2. Process the new HTML template
            String htmlContent = templateEngine.process("email-verification-template", context);

            // 3. Create and send the HTML email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.addInline("logoImage", new ClassPathResource("static/images/logo.svg"));

            mailSender.send(mimeMessage);
            System.out.println("HTML Verification email sent to " + to);

        } catch (MessagingException e) {
            System.err.println("Failed to send verification email: " + e.getMessage());
        }
    }


    @Async
public void sendUniversityVerificationEmail(String to, String token) {
    try {
        String verificationUrl = frontendUrl + "/verify-university?token=" + token; // A new frontend route
        String subject = "Verify Your Student Status";

        Context context = new Context();
        context.setVariable("verificationUrl", verificationUrl);
        context.setVariable("subject", subject);

        // You can create a new Thymeleaf template for this email
        String htmlContent = templateEngine.process("university-verification-template", context);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
        System.out.println("University verification email sent to " + to);

    } catch (MessagingException e) {
        System.err.println("Failed to send university verification email: " + e.getMessage());
    }
}
}