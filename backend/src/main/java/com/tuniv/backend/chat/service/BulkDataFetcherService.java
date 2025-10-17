package com.tuniv.backend.chat.service;

import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
import com.tuniv.backend.chat.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkDataFetcherService {
    
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final ReactionRepository reactionRepository;
    
    public Map<Integer, List<ConversationParticipant>> getParticipantsByConversations(List<Conversation> conversations) {
        if (conversations.isEmpty()) {
            return Map.of();
        }
        
        List<Integer> conversationIds = conversations.stream()
            .map(Conversation::getConversationId)
            .collect(Collectors.toList());
        
        List<ConversationParticipant> participants = participantRepository
            .findByConversation_ConversationIdInAndIsActiveTrue(conversationIds);
        
        return participants.stream()
            .collect(Collectors.groupingBy(
                participant -> participant.getConversation().getConversationId()
            ));
    }
    
    public Map<Integer, List<Message>> getPinnedMessagesByConversations(List<Conversation> conversations) {
        if (conversations.isEmpty()) {
            return Map.of();
        }
        
        List<Integer> conversationIds = conversations.stream()
            .map(Conversation::getConversationId)
            .collect(Collectors.toList());
        
        List<Message> pinnedMessages = messageRepository
            .findByConversation_ConversationIdInAndPinnedTrueAndDeletedFalse(conversationIds);
        
        return pinnedMessages.stream()
            .collect(Collectors.groupingBy(
                message -> message.getConversation().getConversationId()
            ));
    }
    
    public Map<Integer, ConversationParticipant> getCurrentUserParticipants(
        List<Conversation> conversations, 
        Integer currentUserId
    ) {
        if (conversations.isEmpty() || currentUserId == null) {
            return Map.of();
        }
        
        List<Integer> conversationIds = conversations.stream()
            .map(Conversation::getConversationId)
            .collect(Collectors.toList());
        
        List<ConversationParticipant> participants = participantRepository
            .findByConversation_ConversationIdInAndUser_UserIdAndIsActiveTrue(conversationIds, currentUserId);
        
        return participants.stream()
            .collect(Collectors.toMap(
                participant -> participant.getConversation().getConversationId(),
                participant -> participant
            ));
    }
    
    public Map<Integer, List<Reaction>> getReactionsByMessages(List<Message> messages) {
        if (messages.isEmpty()) {
            return Map.of();
        }
        
        List<Integer> messageIds = messages.stream()
            .map(Message::getId)
            .collect(Collectors.toList());
        
        List<Reaction> reactions = reactionRepository
            .findByMessage_IdInAndIsRemovedFalse(messageIds);
        
        return reactions.stream()
            .collect(Collectors.groupingBy(
                reaction -> reaction.getMessage().getId()
            ));
    }
    
    public Map<Integer, Integer> getUnreadCountsByConversations(List<Conversation> conversations, Integer currentUserId) {
        if (conversations.isEmpty() || currentUserId == null) {
            return Map.of();
        }
        
        List<Integer> conversationIds = conversations.stream()
            .map(Conversation::getConversationId)
            .collect(Collectors.toList());
        
        List<ConversationParticipant> participants = participantRepository
            .findByConversation_ConversationIdInAndUser_UserIdAndIsActiveTrue(conversationIds, currentUserId);
        
        return participants.stream()
            .collect(Collectors.toMap(
                participant -> participant.getConversation().getConversationId(),
                ConversationParticipant::getUnreadCount
            ));
    }
}