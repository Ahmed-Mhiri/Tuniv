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

import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.follow.model.Follow;
import com.tuniv.backend.follow.model.FollowableType;
import com.tuniv.backend.follow.repository.FollowRepository;
import com.tuniv.backend.notification.dto.NotificationDto;
import com.tuniv.backend.notification.event.NewAnswerEvent;
import com.tuniv.backend.notification.event.NewCommentEvent;
import com.tuniv.backend.notification.event.NewFollowerEvent;
import com.tuniv.backend.notification.event.NewMessageEvent;
import com.tuniv.backend.notification.event.NewQuestionEvent;
import com.tuniv.backend.notification.event.NewQuestionInUniversityEvent;
import com.tuniv.backend.notification.event.NewVoteEvent;
import com.tuniv.backend.notification.event.SolutionMarkedEvent;
import com.tuniv.backend.notification.event.UserJoinedUniversityEvent;
import com.tuniv.backend.notification.mapper.NotificationMapper;
import com.tuniv.backend.notification.model.Notification;
import com.tuniv.backend.notification.model.NotificationType;
import com.tuniv.backend.notification.repository.NotificationRepository;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.university.model.Module;
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
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final SimpMessageSendingOperations messagingTemplate;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    // ✅ NEW: Handle when someone follows you
    @Async
    @EventListener
    public void handleNewFollower(NewFollowerEvent event) {
        User follower = event.getFollower();
        User followedUser = event.getFollowedUser();
        
        if (follower.equals(followedUser)) return;
        
        String message = follower.getUsername() + " started following you";
        String link = "/users/" + follower.getUserId();
        
        createNotificationAndSendEmail(followedUser, follower, NotificationType.NEW_FOLLOWER, message, link);
    }

    // ✅ NEW: Handle new question from followed user
    @Async
    @EventListener
    public void handleNewQuestionFromFollowedUser(NewQuestionEvent event) {
        Question question = event.getQuestion();
        User author = question.getAuthor();
        
        // Get all users who follow this author
        List<Follow> followers = followRepository.findByTargetTypeAndTargetId(
            FollowableType.USER, author.getUserId()
        );
        
        String message = author.getUsername() + " asked a new question: \"" + truncate(question.getTitle(), 30) + "\"";
        String link = "/questions/" + question.getId();
        
        followers.forEach(follow -> {
            User recipient = follow.getUser();
            if (!recipient.equals(author)) {
                createNotificationAndSendEmail(recipient, author, NotificationType.NEW_QUESTION_FROM_FOLLOWED_USER, message, link);
            }
        });
    }

    // ✅ NEW: Handle new question in followed community
    @Async
    @EventListener
    public void handleNewQuestionInFollowedCommunity(NewQuestionEvent event) {
        Question question = event.getQuestion();
        if (question.getCommunity() == null) return;
        
        Community community = question.getCommunity();
        User author = question.getAuthor();
        
        // Get all users who follow this community
        List<Follow> followers = followRepository.findByTargetTypeAndTargetId(
            FollowableType.COMMUNITY, community.getCommunityId()
        );
        
        String message = "New question in " + community.getName() + ": \"" + truncate(question.getTitle(), 30) + "\"";
        String link = "/questions/" + question.getId();
        
        followers.forEach(follow -> {
            User recipient = follow.getUser();
            if (!recipient.equals(author)) {
                createNotificationAndSendEmail(recipient, author, NotificationType.NEW_QUESTION_IN_FOLLOWED_COMMUNITY, message, link);
            }
        });
    }

    // ✅ NEW: Handle new question in followed module
    @Async
    @EventListener
    public void handleNewQuestionInFollowedModule(NewQuestionEvent event) {
        Question question = event.getQuestion();
        if (question.getModule() == null) return;
        
        Module module = question.getModule();
        User author = question.getAuthor();
        
        // Get all users who follow this module
        List<Follow> followers = followRepository.findByTargetTypeAndTargetId(
            FollowableType.MODULE, module.getModuleId()
        );
        
        String message = "New question in " + module.getName() + ": \"" + truncate(question.getTitle(), 30) + "\"";
        String link = "/questions/" + question.getId();
        
        followers.forEach(follow -> {
            User recipient = follow.getUser();
            if (!recipient.equals(author)) {
                createNotificationAndSendEmail(recipient, author, NotificationType.NEW_QUESTION_IN_FOLLOWED_MODULE, message, link);
            }
        });
    }

    // ✅ NEW: Handle new question with followed tag
    @Async
    @EventListener
    public void handleNewQuestionWithFollowedTag(NewQuestionEvent event) {
        Question question = event.getQuestion();
        User author = question.getAuthor();
        
        // For each tag in the question, notify users who follow that tag
        question.getTags().forEach(tag -> {
            List<Follow> followers = followRepository.findByTargetTypeAndTargetId(
                FollowableType.TAG, tag.getId()
            );
            
            String message = "New question with tag #" + tag.getName() + ": \"" + truncate(question.getTitle(), 30) + "\"";
            String link = "/questions/" + question.getId();
            
            followers.forEach(follow -> {
                User recipient = follow.getUser();
                if (!recipient.equals(author)) {
                    createNotificationAndSendEmail(recipient, author, NotificationType.NEW_QUESTION_WITH_FOLLOWED_TAG, message, link);
                }
            });
        });
    }

    @Async
    @EventListener
    public void handleNewAnswer(NewAnswerEvent event) {
        var answer = event.getAnswer();
        var actor = answer.getAuthor();
        var recipient = answer.getQuestion().getAuthor();
        if (actor.equals(recipient)) return;
        var message = actor.getUsername() + " answered your question: \"" + truncate(answer.getQuestion().getTitle(), 40) + "\"";
        var link = "/questions/" + answer.getQuestion().getId() + "?answerId=" + answer.getId();
        createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_ANSWER, message, link);
    }

    @Async
    @EventListener
    public void handleNewComment(NewCommentEvent event) {
        var comment = event.getComment();
        var actor = comment.getAuthor();
        var answer = comment.getAnswer();
        var question = answer.getQuestion();

        if (comment.getParentComment() != null) {
            var recipient = comment.getParentComment().getAuthor();
            if (actor.equals(recipient)) return;
            var message = actor.getUsername() + " replied to your comment.";
            var link = "/questions/" + question.getId() + "?commentId=" + comment.getId() + "#comment-" + comment.getId();
            createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_REPLY_TO_COMMENT, message, link);
        } else {
            var recipient = answer.getAuthor();
            if (actor.equals(recipient)) return;
            var message = actor.getUsername() + " commented on your answer for \"" + truncate(question.getTitle(), 30) + "\".";
            var link = "/questions/" + question.getId() + "?answerId=" + answer.getId() + "#comment-" + comment.getId();
            createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_COMMENT_ON_ANSWER, message, link);
        }
    }

    @Async
    @EventListener
    public void handleNewVote(NewVoteEvent event) {
        userRepository.findById(event.getAuthorId()).ifPresent(recipient -> {
            userRepository.findById(event.getVoterId()).ifPresent(actor -> {
                if (actor.equals(recipient)) return;
                String message = actor.getUsername() + " upvoted your " + event.getPostType().toString().toLowerCase() + ".";
                String link = "/questions/" + event.getQuestionId();
                NotificationType type;
                switch (event.getPostType()) {
                    case QUESTION: type = NotificationType.NEW_VOTE_ON_QUESTION; break;
                    case ANSWER: type = NotificationType.NEW_VOTE_ON_ANSWER; break;
                    case COMMENT: type = NotificationType.NEW_VOTE_ON_COMMENT; break;
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
        var actor = answer.getQuestion().getAuthor();
        if (actor.equals(recipient)) return;
        var message = "Your answer to \"" + truncate(answer.getQuestion().getTitle(), 30) + "\" was marked as the solution!";
        var link = "/questions/" + answer.getQuestion().getId() + "?answerId=" + answer.getId();
        createNotificationAndSendEmail(recipient, actor, NotificationType.ANSWER_MARKED_AS_SOLUTION, message, link);
    }

    @Async
    @EventListener
    public void handleNewQuestion(NewQuestionInUniversityEvent event) {
        var question = event.getQuestion();
        var actor = question.getAuthor();
        var university = question.getModule().getUniversity();
        List<User> recipients = userRepository.findAllMembersOfUniversityExcludingAuthor(
                university.getUniversityId(),
                actor.getUserId()
        );
        var message = "A new question was asked in " + university.getName() + ": \"" + truncate(question.getTitle(), 30) + "\"";
        var link = "/questions/" + question.getId();
        recipients.forEach(recipient ->
                createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_QUESTION_IN_UNI, message, link)
        );
    }

    @Async
    @EventListener
    public void handleNewMessage(NewMessageEvent event) {
        Message message = event.getMessage();
        User actor = message.getAuthor();
        List<User> recipients = event.getRecipients();

        if (actor == null || recipients == null || recipients.isEmpty()) {
            log.warn("handleNewMessage event received with null actor or empty recipients.");
            return;
        }

        String link = "/chat/" + message.getConversation().getConversationId();
        String messageText = String.format("You have a new message from %s.", actor.getUsername());

        recipients.forEach(recipient -> {
            createNotificationAndSendEmail(
                    recipient,
                    actor,
                    NotificationType.NEW_CHAT_MESSAGE,
                    messageText,
                    link
            );
        });
    }

    @Async
    @EventListener
    public void handleUserJoinedUniversity(UserJoinedUniversityEvent event) {
        var recipient = event.getUser();
        var university = event.getUniversity();
        var message = "Welcome to " + university.getName() + "! We're glad to have you.";
        var link = "/universities/" + university.getUniversityId();
        sendEmail(recipient, null, NotificationType.WELCOME_TO_UNIVERSITY, message, link);
    }

    private void createNotificationAndSendEmail(User recipient, User actor, NotificationType type, String message, String link) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .message(message)
                .link(link)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        String destination = "/topic/user/" + recipient.getUserId() + "/notifications";
        NotificationDto notificationDto = NotificationMapper.toDto(notification);
        messagingTemplate.convertAndSend(destination, notificationDto);
        log.info("Sent WebSocket notification to destination: {}", destination);

        sendEmail(recipient, actor, type, message, link);
    }

    private void sendEmail(User recipient, User actor, NotificationType type, String message, String link) {
        try {
            Context context = new Context();
            String subject = "You have a new notification";
            String templateName = "default-notification-template";

            switch (type) {
                case NEW_ANSWER:
                    subject = "New Answer for your question";
                    templateName = "new-answer-template";
                    break;
                case NEW_CHAT_MESSAGE:
                    subject = "New Chat Message from " + (actor != null ? actor.getUsername() : "a user");
                    templateName = "new-message-template";
                    break;
                case NEW_FOLLOWER:
                    subject = "You have a new follower";
                    templateName = "new-follower-template";
                    break;
                case NEW_QUESTION_FROM_FOLLOWED_USER:
                    subject = "New question from " + (actor != null ? actor.getUsername() : "a user you follow");
                    templateName = "new-question-template";
                    break;
                case NEW_QUESTION_IN_FOLLOWED_COMMUNITY:
                case NEW_QUESTION_IN_FOLLOWED_MODULE:
                case NEW_QUESTION_WITH_FOLLOWED_TAG:
                    subject = "New content in your feed";
                    templateName = "new-content-template";
                    break;
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

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsForUser(UserDetailsImpl currentUser) {
        List<Notification> notifications = notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(currentUser.getId());
        return notifications.stream()
                .map(NotificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Integer notificationId, UserDetailsImpl currentUser) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        if (!notification.getRecipient().getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to update this notification.");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UserDetailsImpl currentUser) {
        notificationRepository.markAllAsReadForUser(currentUser.getId());
    }

    @Transactional
    public void deleteNotification(Integer notificationId, String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        notificationRepository.deleteByNotificationIdAndRecipient_UserId(notificationId, currentUser.getUserId());
    }

    @Transactional
    public void deleteAllNotifications(String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        notificationRepository.deleteAllByRecipient_UserId(currentUser.getUserId());
    }
}