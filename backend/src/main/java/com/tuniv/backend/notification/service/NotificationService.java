package com.tuniv.backend.notification.service;

import com.tuniv.backend.qa.event.NewAnswerEvent; // We will create this event class next
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    @Async // This ensures the method runs in a separate thread
    @EventListener
    public void handleNewAnswerEvent(NewAnswerEvent event) {
        // This method will be triggered automatically when a NewAnswerEvent is published.
        System.out.println("New answer event received for question: " + event.getQuestionTitle());
        
        // Create the email
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(event.getQuestionAuthorEmail());
        email.setSubject("New Answer for your question: \"" + event.getQuestionTitle() + "\"");
        email.setText(
            "Hello,\n\n" +
            "A new answer has been posted to your question by " + event.getAnswerAuthorUsername() + ".\n\n" +
            "Go to the platform to check it out!\n\n" +
            "Regards,\nThe Tuniv Team"
        );
        
        // Send the email
        mailSender.send(email);
        
        System.out.println("Notification email sent to " + event.getQuestionAuthorEmail());
    }
}