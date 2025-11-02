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
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.follow.model.Follow;
import com.tuniv.backend.follow.model.FollowableType;
import com.tuniv.backend.follow.repository.FollowRepository;
import com.tuniv.backend.notification.dto.NotificationDto;
import com.tuniv.backend.notification.event.NewAnswerEvent;
import com.tuniv.backend.notification.event.NewChatMessageReactionEvent;
import com.tuniv.backend.notification.event.NewCommentEvent;
import com.tuniv.backend.notification.event.NewFollowerEvent;
import com.tuniv.backend.notification.event.NewMessageEvent;
import com.tuniv.backend.notification.event.NewTopicEvent;
import com.tuniv.backend.notification.event.NewVoteEvent;
import com.tuniv.backend.notification.event.SolutionAcceptedEvent;
import com.tuniv.backend.notification.event.SolutionUnmarkedEvent;
import com.tuniv.backend.notification.event.UserJoinedUniversityEvent;
import com.tuniv.backend.notification.mapper.NotificationMapper;
import com.tuniv.backend.notification.model.Notification;
import com.tuniv.backend.notification.model.NotificationType;
import com.tuniv.backend.notification.repository.NotificationRepository;
import com.tuniv.backend.qa.model.PostType;
import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.model.University;
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

    // ✅ UPDATED: Handle when someone follows you
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

    // ✅ UPDATED: Handle new topic from followed user
    @Async
    @EventListener
    public void handleNewTopicFromFollowedUser(NewTopicEvent event) {
        Topic topic = event.getTopic();
        User author = topic.getAuthor();
        
        // Get all users who follow this author
        List<Follow> followers = followRepository.findByTargetTypeAndTargetId(
            FollowableType.USER, author.getUserId()
        );
        
        String message = author.getUsername() + " created a new " + topic.getTopicType().toString().toLowerCase() + ": \"" + truncate(topic.getTitle(), 30) + "\"";
        String link = "/topics/" + topic.getId();
        
        followers.forEach(follow -> {
            User recipient = follow.getUser();
            if (!recipient.equals(author)) {
                createNotificationAndSendEmail(recipient, author, NotificationType.NEW_QUESTION_FROM_FOLLOWED_USER, message, link);
            }
        });
    }

    // ✅ UPDATED: Handle new topic in followed community
    @Async
    @EventListener
    public void handleNewTopicInFollowedCommunity(NewTopicEvent event) {
        Topic topic = event.getTopic();
        if (topic.getCommunity() == null) return;
        
        Community community = topic.getCommunity();
        User author = topic.getAuthor();
        
        // Get all users who follow this community
        List<Follow> followers = followRepository.findByTargetTypeAndTargetId(
            FollowableType.COMMUNITY, community.getCommunityId()
        );
        
        String message = "New " + topic.getTopicType().toString().toLowerCase() + " in " + community.getName() + ": \"" + truncate(topic.getTitle(), 30) + "\"";
        String link = "/topics/" + topic.getId();
        
        followers.forEach(follow -> {
            User recipient = follow.getUser();
            if (!recipient.equals(author)) {
                createNotificationAndSendEmail(recipient, author, NotificationType.NEW_QUESTION_IN_FOLLOWED_COMMUNITY, message, link);
            }
        });
    }

    // ✅ UPDATED: Handle new topic in followed module
    @Async
    @EventListener
    public void handleNewTopicInFollowedModule(NewTopicEvent event) {
        Topic topic = event.getTopic();
        if (topic.getModule() == null) return;
        
        Module module = topic.getModule();
        User author = topic.getAuthor();
        
        // Get all users who follow this module
        List<Follow> followers = followRepository.findByTargetTypeAndTargetId(
            FollowableType.MODULE, module.getModuleId()
        );
        
        String message = "New " + topic.getTopicType().toString().toLowerCase() + " in " + module.getName() + ": \"" + truncate(topic.getTitle(), 30) + "\"";
        String link = "/topics/" + topic.getId();
        
        followers.forEach(follow -> {
            User recipient = follow.getUser();
            if (!recipient.equals(author)) {
                createNotificationAndSendEmail(recipient, author, NotificationType.NEW_QUESTION_IN_FOLLOWED_MODULE, message, link);
            }
        });
    }

    // ✅ UPDATED: Handle new topic with followed tag
    @Async
    @EventListener
    public void handleNewTopicWithFollowedTag(NewTopicEvent event) {
        Topic topic = event.getTopic();
        User author = topic.getAuthor();
        
        // For each tag in the topic, notify users who follow that tag
        topic.getTags().forEach(tag -> {
            List<Follow> followers = followRepository.findByTargetTypeAndTargetId(
                FollowableType.TAG, tag.getId()
            );
            
            String message = "New " + topic.getTopicType().toString().toLowerCase() + " with tag #" + tag.getName() + ": \"" + truncate(topic.getTitle(), 30) + "\"";
            String link = "/topics/" + topic.getId();
            
            followers.forEach(follow -> {
                User recipient = follow.getUser();
                if (!recipient.equals(author)) {
                    createNotificationAndSendEmail(recipient, author, NotificationType.NEW_QUESTION_WITH_FOLLOWED_TAG, message, link);
                }
            });
        });
    }

    // ✅ UPDATED: Handle new answer (reply on QUESTION topic)
    @Async
    @EventListener
    public void handleNewAnswer(NewAnswerEvent event) {
        Reply answer = event.getAnswer();
        User actor = answer.getAuthor();
        User recipient = answer.getTopic().getAuthor();
        
        if (actor.equals(recipient)) return;
        
        String message = actor.getUsername() + " answered your " + answer.getTopic().getTopicType().toString().toLowerCase() + ": \"" + truncate(answer.getTopic().getTitle(), 40) + "\"";
        String link = "/topics/" + answer.getTopic().getId() + "?replyId=" + answer.getId();
        
        createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_ANSWER, message, link);
    }

    // ✅ NEW: Handle new comment (reply on POST topic or nested reply)
    @Async
    @EventListener
    public void handleNewComment(NewCommentEvent event) {
        Reply comment = event.getComment();
        User actor = comment.getAuthor();
        Topic topic = comment.getTopic();

        if (comment.getParentReply() != null) {
            // This is a reply to another comment
            User recipient = comment.getParentReply().getAuthor();
            if (actor.equals(recipient)) return;
            
            String message = actor.getUsername() + " replied to your comment on: \"" + truncate(topic.getTitle(), 30) + "\"";
            String link = "/topics/" + topic.getId() + "?commentId=" + comment.getId() + "#comment-" + comment.getId();
            
            createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_REPLY_TO_COMMENT, message, link);
        } else {
            // This is a comment on a POST topic
            User recipient = topic.getAuthor();
            if (actor.equals(recipient)) return;
            
            String message = actor.getUsername() + " commented on your " + topic.getTopicType().toString().toLowerCase() + ": \"" + truncate(topic.getTitle(), 30) + "\"";
            String link = "/topics/" + topic.getId() + "?commentId=" + comment.getId() + "#comment-" + comment.getId();
            
            createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_COMMENT_ON_ANSWER, message, link);
        }
    }

    // ✅ UPDATED: Handle new vote
    @Async
    @EventListener
    public void handleNewVote(NewVoteEvent event) {
        userRepository.findById(event.getAuthorId()).ifPresent(recipient -> {
            userRepository.findById(event.getVoterId()).ifPresent(actor -> {
                if (actor.equals(recipient)) return;
                
                String postType = getPostTypeDescription(event.getPostType());
                String message = actor.getUsername() + " upvoted your " + postType + " on: \"" + truncate(event.getQuestionTitle(), 30) + "\"";
                String link = "/topics/" + event.getQuestionId();
                
                NotificationType type = getVoteNotificationType(event.getPostType());
                if (type != null) {
                    createNotificationAndSendEmail(recipient, actor, type, message, link);
                }
            });
        });
    }

    // ✅ UPDATED: Handle solution marked
    @Async
    @EventListener
    public void handleSolutionMarked(SolutionAcceptedEvent event) {
        Reply solution = event.getSolution();
        User recipient = solution.getAuthor();
        User actor = solution.getTopic().getAuthor();
        
        if (actor.equals(recipient)) return;
        
        String message = "Your answer to \"" + truncate(solution.getTopic().getTitle(), 30) + "\" was marked as the solution!";
        String link = "/topics/" + solution.getTopic().getId() + "?solutionId=" + solution.getId();
        
        createNotificationAndSendEmail(recipient, actor, NotificationType.ANSWER_MARKED_AS_SOLUTION, message, link);
    }

    // ✅ NEW: Handle solution unmarked
    @Async
    @EventListener
    public void handleSolutionUnmarked(SolutionUnmarkedEvent event) {
        Reply previousSolution = event.getPreviousSolution();
        User recipient = previousSolution.getAuthor();
        User actor = event.getTopic().getAuthor();
        
        if (actor.equals(recipient)) return;
        
        String message = "Your solution to \"" + truncate(event.getTopic().getTitle(), 30) + "\" was unmarked";
        String link = "/topics/" + event.getTopic().getId();
        
        // You might want to create a new NotificationType for this, or use existing
        createNotificationAndSendEmail(recipient, actor, NotificationType.ANSWER_MARKED_AS_SOLUTION, message, link);
    }

    // ✅ UPDATED: Handle new topic in university
    @Async
    @EventListener
    public void handleNewTopicInUniversity(NewTopicEvent event) {
        Topic topic = event.getTopic();
        User actor = topic.getAuthor();
        
        if (topic.getModule() == null || topic.getModule().getUniversity() == null) return;
        
        University university = topic.getModule().getUniversity();
        List<User> recipients = userRepository.findAllMembersOfUniversityExcludingAuthor(
                university.getUniversityId(),
                actor.getUserId()
        );
        
        String message = "A new " + topic.getTopicType().toString().toLowerCase() + " was posted in " + university.getName() + ": \"" + truncate(topic.getTitle(), 30) + "\"";
        String link = "/topics/" + topic.getId();
        
        recipients.forEach(recipient ->
                createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_QUESTION_IN_UNI, message, link)
        );
    }

    // ✅ Keep existing chat message handler
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
    public void handleNewChatMessageReaction(NewChatMessageReactionEvent event) {
        Reaction reaction = event.getReaction();
        User actor = reaction.getUser();
        Message message = (Message) reaction.getPost();
        User recipient = message.getAuthor();

        // Don't create a notification if users react to their own message
        if (actor.equals(recipient)) {
            return;
        }

        // Create a link that directs the user to the specific conversation
        String link = "/chat/" + message.getConversation().getConversationId();

        // Create a descriptive notification message
        String messageText = String.format("%s reacted with %s to your message: \"%s\"",
                actor.getUsername(),
                reaction.getEmoji(),
                truncate(message.getBody(), 30)
        );

        // Create the notification and send the email/websocket push
        createNotificationAndSendEmail(recipient, actor, NotificationType.NEW_REACTION_ON_CHAT_MESSAGE, messageText, link);
    }

    // ✅ Keep existing university join handler
    @Async
    @EventListener
    public void handleUserJoinedUniversity(UserJoinedUniversityEvent event) {
        User recipient = event.getUser();
        University university = event.getUniversity();
        String message = "Welcome to " + university.getName() + "! We're glad to have you.";
        String link = "/universities/" + university.getUniversityId();
        sendEmail(recipient, null, NotificationType.WELCOME_TO_UNIVERSITY, message, link);
    }

    // ✅ NEW: Helper method to get post type description
    private String getPostTypeDescription(PostType postType) {
        switch (postType) {
            case TOPIC: return "topic";
            case REPLY: return "reply";
            default: return "post";
        }
    }

    // ✅ NEW: Helper method to get vote notification type
    private NotificationType getVoteNotificationType(PostType postType) {
        switch (postType) {
            case TOPIC: return NotificationType.NEW_VOTE_ON_QUESTION;
            case REPLY: return NotificationType.NEW_VOTE_ON_ANSWER;
            default: return null;
        }
    }

    // ✅ Keep existing notification creation and email methods
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
                    subject = "New Answer for your " + (actor != null ? "question" : "topic");
                    templateName = "new-answer-template";
                    break;
                case NEW_COMMENT_ON_ANSWER:
                    subject = "New Comment on your " + (actor != null ? "post" : "topic");
                    templateName = "new-comment-template";
                    break;
                case NEW_REPLY_TO_COMMENT:
                    subject = "New Reply to your comment";
                    templateName = "new-reply-template";
                    break;
                case NEW_CHAT_MESSAGE:
                    subject = "New Chat Message from " + (actor != null ? actor.getUsername() : "a user");
                    templateName = "new-message-template";
                    break;
                case NEW_REACTION_ON_CHAT_MESSAGE:
                    subject = "New reaction on your chat message";
                    templateName = "new-reaction-template"; // You can reuse or create a new one
                    break;
                case NEW_FOLLOWER:
                    subject = "You have a new follower";
                    templateName = "new-follower-template";
                    break;
                case NEW_QUESTION_FROM_FOLLOWED_USER:
                    subject = "New content from " + (actor != null ? actor.getUsername() : "a user you follow");
                    templateName = "new-content-template";
                    break;
                case ANSWER_MARKED_AS_SOLUTION:
                    subject = "Your answer was marked as solution!";
                    templateName = "solution-marked-template";
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

    // ✅ Keep existing service methods
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