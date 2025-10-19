package com.tuniv.backend.chat.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.dto.MessageThreadDto;
import com.tuniv.backend.chat.mapper.mapstruct.MessageMapper;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.chat.repository.MessageRepository;
import com.tuniv.backend.chat.service.BulkDataFetcherService;
import com.tuniv.backend.chat.service.EntityFinderService;
import com.tuniv.backend.chat.service.MessageThreadService;
import com.tuniv.backend.chat.service.ReactionService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageThreadServiceImpl implements MessageThreadService {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final EntityFinderService entityFinderService;
    private final BulkDataFetcherService bulkDataFetcherService;
    private final ReactionService reactionService;

    @Override
    public Page<ChatMessageDto> getMessageReplies(Integer parentMessageId, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching replies for parent message {} by user {}", parentMessageId, currentUser.getId());
        
        Message parentMessage = entityFinderService.getMessageOrThrow(parentMessageId);
        validateConversationMembership(parentMessage.getConversation(), currentUser);
        
        // Create specification to find replies to the parent message
        Specification<Message> spec = (root, query, cb) -> 
            cb.and(
                cb.equal(root.get("replyToMessage").get("id"), parentMessageId),
                cb.equal(root.get("deleted"), false)
            );
        
        Page<Message> replies = messageRepository.findAll(spec, pageable);
        
        if (replies.isEmpty()) {
            log.debug("No replies found for parent message {}", parentMessageId);
            return Page.empty(pageable);
        }
        
        // Use optimized bulk mapper for replies to avoid N+1 queries
        List<Message> replyList = replies.getContent();
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(replyList);
        
        List<ChatMessageDto> dtos = new ArrayList<>();
        for (Message reply : replyList) {
            List<Reaction> messageReactions = reactionsByMessage.getOrDefault(reply.getId(), List.of());
            // Use ReactionService for reaction calculation
            MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
                messageReactions, currentUser.getId(), reply.getId());
            dtos.add(messageMapper.toChatMessageDto(reply, reactionsSummary));
        }
        
        log.debug("Found {} replies for parent message {}", dtos.size(), parentMessageId);
        return new PageImpl<>(dtos, pageable, replies.getTotalElements());
    }

    @Override
    public MessageThreadDto getMessageThread(Integer messageId, UserDetailsImpl currentUser) {
        log.debug("Fetching full thread for message {} by user {}", messageId, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        MessageThreadDto thread = new MessageThreadDto();
        
        // Determine if the message is a reply or a parent
        Message parentMessage = message.getReplyToMessage();
        Message threadStarter = (parentMessage != null) ? parentMessage : message;
        
        // Use bulk mapper for the thread starter message
        List<Message> threadStarterList = List.of(threadStarter);
        Map<Integer, List<Reaction>> starterReactions = bulkDataFetcherService.getReactionsByMessages(threadStarterList);
        List<Reaction> starterMessageReactions = starterReactions.getOrDefault(threadStarter.getId(), List.of());
        
        // Use ReactionService for reaction calculation
        MessageReactionsSummaryDto starterReactionsSummary = reactionService.calculateReactionsSummary(
            starterMessageReactions, currentUser.getId(), threadStarter.getId());
        
        // Set the parent message (thread starter)
        if (parentMessage != null) {
            // If the requested message is a reply, set the parent
            thread.setParentMessage(messageMapper.toChatMessageDto(threadStarter, starterReactionsSummary));
        } else {
            // If the requested message is the thread starter, set it as parent
            thread.setParentMessage(messageMapper.toChatMessageDto(threadStarter, starterReactionsSummary));
        }
        
        // Get all replies to the thread starter with bulk mapping
        Page<ChatMessageDto> repliesPage = getMessageReplies(
            threadStarter.getId(), 
            currentUser, 
            Pageable.ofSize(50) // Limit replies for thread view
        );
        
        thread.setReplies(repliesPage.getContent());
        thread.setReplyCount((int) repliesPage.getTotalElements());
        thread.setThreadStarterId(threadStarter.getId());
        
        // Additional thread metadata
        thread.setConversationId(message.getConversation().getConversationId());
        thread.setHasMoreReplies(repliesPage.getTotalElements() > repliesPage.getContent().size());
        
        log.debug("Thread for message {} loaded with {} replies", messageId, thread.getReplyCount());
        return thread;
    }

    /**
     * Gets message thread with custom page size for replies
     */
    public MessageThreadDto getMessageThread(Integer messageId, UserDetailsImpl currentUser, int replyPageSize) {
        log.debug("Fetching full thread for message {} with custom page size {} by user {}", 
                 messageId, replyPageSize, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        MessageThreadDto thread = new MessageThreadDto();
        
        // Determine thread structure
        Message parentMessage = message.getReplyToMessage();
        Message threadStarter = (parentMessage != null) ? parentMessage : message;
        
        // Use bulk mapper for the thread starter message
        List<Message> threadStarterList = List.of(threadStarter);
        Map<Integer, List<Reaction>> starterReactions = bulkDataFetcherService.getReactionsByMessages(threadStarterList);
        List<Reaction> starterMessageReactions = starterReactions.getOrDefault(threadStarter.getId(), List.of());
        
        // Use ReactionService for reaction calculation
        MessageReactionsSummaryDto starterReactionsSummary = reactionService.calculateReactionsSummary(
            starterMessageReactions, currentUser.getId(), threadStarter.getId());
        
        // Set the parent message
        if (parentMessage != null) {
            thread.setParentMessage(messageMapper.toChatMessageDto(threadStarter, starterReactionsSummary));
        } else {
            thread.setParentMessage(messageMapper.toChatMessageDto(threadStarter, starterReactionsSummary));
        }
        
        // Get replies with custom page size
        Page<ChatMessageDto> repliesPage = getMessageReplies(
            threadStarter.getId(), 
            currentUser, 
            Pageable.ofSize(replyPageSize)
        );
        
        thread.setReplies(repliesPage.getContent());
        thread.setReplyCount((int) repliesPage.getTotalElements());
        thread.setThreadStarterId(threadStarter.getId());
        thread.setConversationId(message.getConversation().getConversationId());
        thread.setHasMoreReplies(repliesPage.getTotalElements() > repliesPage.getContent().size());
        
        return thread;
    }

    /**
     * Gets only the direct replies to a message (without building full thread structure)
     */
    public Page<ChatMessageDto> getDirectReplies(Integer messageId, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching direct replies for message {} by user {}", messageId, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        Specification<Message> spec = (root, query, cb) -> 
            cb.and(
                cb.equal(root.get("replyToMessage").get("id"), messageId),
                cb.equal(root.get("deleted"), false)
            );
        
        Page<Message> replies = messageRepository.findAll(spec, pageable);
        
        if (replies.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // Use optimized bulk mapper
        List<Message> replyList = replies.getContent();
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(replyList);
        
        List<ChatMessageDto> dtos = new ArrayList<>();
        for (Message reply : replyList) {
            List<Reaction> messageReactions = reactionsByMessage.getOrDefault(reply.getId(), List.of());
            MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
                messageReactions, currentUser.getId(), reply.getId());
            dtos.add(messageMapper.toChatMessageDto(reply, reactionsSummary));
        }
        
        return new PageImpl<>(dtos, pageable, replies.getTotalElements());
    }

    /**
     * Checks if a message is part of a thread (either has replies or is a reply itself)
     */
    public boolean isMessagePartOfThread(Integer messageId, UserDetailsImpl currentUser) {
        try {
            Message message = entityFinderService.getMessageOrThrow(messageId);
            validateConversationMembership(message.getConversation(), currentUser);
            
            // Check if message has replies or is itself a reply
            boolean hasReplies = messageRepository.countByReplyToMessageAndDeletedFalse(message) > 0;
            boolean isReply = message.getReplyToMessage() != null;
            
            return hasReplies || isReply;
        } catch (Exception e) {
            log.warn("Error checking if message {} is part of thread: {}", messageId, e.getMessage());
            return false;
        }
    }

    /**
     * Gets the thread starter message for any message in a thread
     */
    public ChatMessageDto getThreadStarter(Integer messageId, UserDetailsImpl currentUser) {
        log.debug("Finding thread starter for message {} by user {}", messageId, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        Message threadStarter = findThreadStarter(message);
        
        // Use bulk mapper for the thread starter
        List<Message> threadStarterList = List.of(threadStarter);
        Map<Integer, List<Reaction>> reactions = bulkDataFetcherService.getReactionsByMessages(threadStarterList);
        List<Reaction> messageReactions = reactions.getOrDefault(threadStarter.getId(), List.of());
        
        MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
            messageReactions, currentUser.getId(), threadStarter.getId());
        
        return messageMapper.toChatMessageDto(threadStarter, reactionsSummary);
    }

    // ========== Private Helper Methods ==========
    
    private void validateConversationMembership(com.tuniv.backend.chat.model.Conversation conversation, UserDetailsImpl user) {
        try {
            entityFinderService.getConversationParticipantOrThrow(conversation.getConversationId(), user.getId());
        } catch (Exception e) {
            throw new AccessDeniedException("You are not a member of this conversation");
        }
    }

    /**
     * Recursively finds the thread starter message by traversing up the reply chain
     */
    private Message findThreadStarter(Message message) {
        if (message.getReplyToMessage() == null) {
            return message; // This is the thread starter
        }
        return findThreadStarter(message.getReplyToMessage());
    }
}