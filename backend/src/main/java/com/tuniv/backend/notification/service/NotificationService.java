package com.tuniv.backend.notification.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.dto.NotificationDto;
import com.tuniv.backend.notification.event.NewAnswerEvent;
import com.tuniv.backend.notification.event.NewCommentEvent;
import com.tuniv.backend.notification.event.NewMessageEvent;
import com.tuniv.backend.notification.event.NewQuestionInUniversityEvent;
import com.tuniv.backend.notification.event.NewVoteEvent;
import com.tuniv.backend.notification.event.SolutionMarkedEvent;
import com.tuniv.backend.notification.event.UserJoinedUniversityEvent;
import com.tuniv.backend.notification.mapper.NotificationMapper;
import com.tuniv.backend.notification.model.Notification;
import com.tuniv.backend.notification.model.NotificationType;
import com.tuniv.backend.notification.repository.NotificationRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final SimpMessageSendingOperations messagingTemplate; // 👈 --- INJECT THIS


    @Value("${app.frontend.url}")
    private String frontendUrl;

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    // =========================================================================
    // ✅ EVENT LISTENERS
    // =========================================================================

    @Async
    @EventListener
    public void handleNewAnswer(NewAnswerEvent event) {
        var answer = event.getAnswer();
        var actor = answer.getAuthor();
        var recipient = answer.getQuestion().getAuthor();
        
        if (actor.equals(recipient)) return;

        var message = actor.getUsername() + " answered your question: \"" + truncate(answer.getQuestion().getTitle(), 40) + "\"";
        var link = "/questions/" + answer.getQuestion().getQuestionId() + "?answerId=" + answer.getAnswerId();

        createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_ANSWER, message, link);
    }
    
    @Async
    @EventListener
    public void handleNewComment(NewCommentEvent event) {
        var comment = event.getComment();
        var actor = comment.getAuthor();
        var answer = comment.getAnswer();
        var question = answer.getQuestion();

        // Case A: Reply to another comment
        if (comment.getParentComment() != null) {
            var recipient = comment.getParentComment().getAuthor();
            if (actor.equals(recipient)) return;

            var message = actor.getUsername() + " replied to your comment.";
            var link = "/questions/" + question.getQuestionId() + "?commentId=" + comment.getCommentId() + "#comment-" + comment.getCommentId();
            createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_REPLY_TO_COMMENT, message, link);
        }
        // Case B: New comment on an answer
        else {
            var recipient = answer.getAuthor();
            if (actor.equals(recipient)) return;

            var message = actor.getUsername() + " commented on your answer for \"" + truncate(question.getTitle(), 30) + "\".";
            var link = "/questions/" + question.getQuestionId() + "?answerId=" + answer.getAnswerId() + "#comment-" + comment.getCommentId();
            createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_COMMENT_ON_ANSWER, message, link);
        }
    }
    
    @Async
    @EventListener
    public void handleNewVote(NewVoteEvent event) {
        // Using Optional.ofNullable to handle potential missing users gracefully
        userRepository.findById(event.getAuthorId()).ifPresent(recipient -> {
            userRepository.findById(event.getVoterId()).ifPresent(actor -> {
                
                if (actor.equals(recipient)) return;

                String message = actor.getUsername() + " upvoted your " + event.getPostType().toString().toLowerCase() + ".";
                String link = "/questions/" + event.getQuestionId();
                NotificationType type;

                switch (event.getPostType()) {
                    case QUESTION: type = NotificationType.NEW_VOTE_ON_QUESTION; break;
                    case ANSWER:   type = NotificationType.NEW_VOTE_ON_ANSWER; break;
                    case COMMENT:  type = NotificationType.NEW_VOTE_ON_COMMENT; break;
                    default: return;
                }
                createNotificationAndSendEmail(recipient, actor, type, message, link);
            });
        });
    }

    @Async
    @EventListener
    public void handleSolutionMarked(SolutionMarkedEvent event) {
        var answer = event.getAnswer();
        var recipient = answer.getAuthor();
        var actor = answer.getQuestion().getAuthor(); // The one who marked it

        if (actor.equals(recipient)) return;

        var message = "Your answer to \"" + truncate(answer.getQuestion().getTitle(), 30) + "\" was marked as the solution!";
        var link = "/questions/" + answer.getQuestion().getQuestionId() + "?answerId=" + answer.getAnswerId();
        createNotificationAndSendEmail(recipient, actor, NotificationType.ANSWER_MARKED_AS_SOLUTION, message, link);
    }
    
    @Async
    @EventListener
    public void handleNewQuestion(NewQuestionInUniversityEvent event) {
        var question = event.getQuestion();
        var actor = question.getAuthor();
        var university = question.getModule().getUniversity();

        // Fetch all members, excluding the question author
        List<User> recipients = university.getMembers() 
            .stream()
            .map(membership -> membership.getUser())
            .filter(user -> !user.equals(actor))
            .collect(Collectors.toList());

        var message = "A new question was asked in " + university.getName() + ": \"" + truncate(question.getTitle(), 30) + "\"";
        var link = "/questions/" + question.getQuestionId();
        
        recipients.forEach(recipient -> 
            createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_QUESTION_IN_UNI, message, link)
        );
    }
    
    @Async
    @EventListener
    public void handleNewMessage(NewMessageEvent event) {
        var message = event.getMessage();
        var actor = message.getSender();
        var conversation = message.getConversation();

        conversation.getParticipants().stream()
            .map(p -> p.getUser())
            .filter(user -> !user.equals(actor))
            .forEach(recipient -> {
                var messageText = "You have a new message from " + actor.getUsername() + ".";
                var link = "/chat/" + conversation.getConversationId();
                createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_CHAT_MESSAGE, messageText, link);
            });
    }

    @Async
    @EventListener
    public void handleUserJoinedUniversity(UserJoinedUniversityEvent event) {
        var recipient = event.getUser();
        var university = event.getUniversity();
        
        var message = "Welcome to " + university.getName() + "! We're glad to have you.";
        var link = "/universities/" + university.getUniversityId(); // Link to the university page
        
        // This is a welcome notification, so it has no "actor" and only sends an email.
        sendEmail(recipient, null, NotificationType.WELCOME_TO_UNIVERSITY, message, link);
    }

    // =========================================================================
    // ✅ CORE LOGIC
    // =========================================================================

    private void createNotificationAndSendEmail(User recipient, User actor, NotificationType type, String message, String link) {
        // 1. Create and save the in-app notification
        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .message(message)
                .link(link)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        // 2. Push a real-time update via WebSockets
        // The destination is specific to the recipient's username.
        String destination = "/topic/notifications/" + recipient.getUsername();
        NotificationDto notificationDto = NotificationMapper.toDto(notification);

        messagingTemplate.convertAndSend(destination, notificationDto);
        log.info("Sent WebSocket notification to destination: {}", destination);


        // 3. Send the corresponding email
        sendEmail(recipient, actor, type, message, link);
    }

    private void sendEmail(User recipient, User actor, NotificationType type, String message, String link) {
        try {
            Context context = new Context();
            String subject = "You have a new notification"; // Default subject
            String templateName = "default-notification-template"; // A generic template

            // Customize email based on notification type
            switch (type) {
                case NEW_ANSWER:
                    subject = "New Answer for your question";
                    templateName = "new-answer-template"; // You'll need to create this HTML file
                    break;
                case NEW_CHAT_MESSAGE:
                    subject = "New Chat Message from " + (actor != null ? actor.getUsername() : "a user");
                    templateName = "new-message-template";
                    break;
                // Add more cases for other types to use specific templates and subjects...
            }
            
            context.setVariable("recipientUsername", recipient.getUsername());
            context.setVariable("actorUsername", actor != null ? actor.getUsername() : "Someone");
            context.setVariable("message", message);
            context.setVariable("actionUrl", frontendUrl + link);
            context.setVariable("subject", subject);

            String htmlContent = templateEngine.process(templateName, context);
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            
            helper.setTo(recipient.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.addInline("logoImage", new ClassPathResource("static/images/logo.svg"));

            mailSender.send(mimeMessage);
            log.info("Notification email sent to {}", recipient.getEmail());

        } catch (MessagingException e) {
            log.error("Failed to send notification email to {}: {}", recipient.getEmail(), e.getMessage());
        }
    }
    // ✅ NEW: PUBLIC API METHODS FOR THE CONTROLLER
    // =========================================================================

    /**
     * Retrieves all notifications for the currently authenticated user.
     *
     * @param currentUser The details of the logged-in user.
     * @return A list of notification DTOs, ordered by most recent.
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsForUser(UserDetailsImpl currentUser) {
        List<Notification> notifications = notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(currentUser.getId());
                
        return notifications.stream()
                .map(NotificationMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Marks a single notification as read.
     * Ensures that a user can only mark their own notifications as read.
     *
     * @param notificationId The ID of the notification to mark as read.
     * @param currentUser The details of the logged-in user.
     */
    @Transactional
    public void markAsRead(Integer notificationId, UserDetailsImpl currentUser) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        // Security Check: Ensure the user owns this notification
        if (!notification.getRecipient().getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to update this notification.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Marks all unread notifications for a user as read.
     *
     * @param currentUser The details of the logged-in user.
     */
    @Transactional
    public void markAllAsRead(UserDetailsImpl currentUser) {
        List<Notification> unreadNotifications = notificationRepository
                .findAllByRecipientUserIdAndIsReadIsFalse(currentUser.getId());

        if (!unreadNotifications.isEmpty()) {
            unreadNotifications.forEach(notification -> notification.setRead(true));
            notificationRepository.saveAll(unreadNotifications);
        }
    }
}