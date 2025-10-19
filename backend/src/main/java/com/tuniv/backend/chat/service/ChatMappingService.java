package com.tuniv.backend.chat.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.dto.PinnedMessageDto;
import com.tuniv.backend.chat.dto.ReadReceiptDto;
import com.tuniv.backend.chat.mapper.mapstruct.MessageMapper;
import com.tuniv.backend.chat.mapper.mapstruct.ReadReceiptMapper;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.Reaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMappingService {
    
    private final MessageMapper messageMapper;
    private final ReadReceiptMapper readReceiptMapper;
    private final ReactionService reactionService;
    private final BulkDataFetcherService bulkDataFetcherService;

    // ========== MESSAGE MAPPING METHODS ==========

    /**
     * Converts a Message to PinnedMessageDto with reactions
     */
    public PinnedMessageDto toPinnedMessageDto(Message message) {
        if (message == null) {
            return null;
        }
        return messageMapper.toPinnedMessageDto(message);
    }

    /**
     * Converts a Message to PinnedMessageDto with reactions summary
     */
    public PinnedMessageDto toPinnedMessageDto(Message message, MessageReactionsSummaryDto reactionsSummary) {
        if (message == null) {
            return null;
        }
        return messageMapper.toPinnedMessageDto(message, reactionsSummary);
    }

    /**
     * Converts a Message to ChatMessageDto with reactions summary
     */
    public ChatMessageDto toChatMessageDto(Message message, MessageReactionsSummaryDto reactionsSummary) {
        if (message == null) {
            return null;
        }
        return messageMapper.toChatMessageDto(message, reactionsSummary);
    }

    /**
     * Converts a list of Messages to PinnedMessageDto list with reactions
     */
    public List<PinnedMessageDto> toPinnedMessageDtoList(List<Message> messages, Integer currentUserId) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        // Bulk fetch reactions for all messages
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(messages);

        return messages.stream()
            .map(message -> {
                List<Reaction> messageReactions = reactionsByMessage.getOrDefault(message.getId(), List.of());
                MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
                    messageReactions, currentUserId, message.getId());
                return toPinnedMessageDto(message, reactionsSummary);
            })
            .collect(Collectors.toList());
    }

    /**
     * Converts a list of Messages to ChatMessageDto list with reactions
     */
    public List<ChatMessageDto> toChatMessageDtoList(List<Message> messages, Integer currentUserId) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        // Bulk fetch reactions for all messages
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(messages);

        return messages.stream()
            .map(message -> {
                List<Reaction> messageReactions = reactionsByMessage.getOrDefault(message.getId(), List.of());
                MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
                    messageReactions, currentUserId, message.getId());
                return toChatMessageDto(message, reactionsSummary);
            })
            .collect(Collectors.toList());
    }

    /**
     * Creates MessageReactionsSummaryDto for a single message
     */
    public MessageReactionsSummaryDto createReactionsSummary(Message message, Integer currentUserId) {
        if (message == null) {
            return null;
        }

        List<Message> singleMessageList = List.of(message);
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(singleMessageList);
        List<Reaction> messageReactions = reactionsByMessage.getOrDefault(message.getId(), List.of());

        return reactionService.calculateReactionsSummary(messageReactions, currentUserId, message.getId());
    }

    // ========== READ RECEIPT MAPPING METHODS ==========

    /**
     * Creates a read receipt DTO from participant and last read message
     */
    public ReadReceiptDto createReadReceiptDto(ConversationParticipant participant, Message lastReadMessage) {
        if (participant == null || lastReadMessage == null) {
            return null;
        }
        
        ReadReceiptDto receipt = readReceiptMapper.toReadReceiptDto(participant);
        receipt.setLastReadTimestamp(participant.getLastReadTimestamp());
        receipt.setLastReadMessageId(lastReadMessage.getId());
        return receipt;
    }
    
    /**
     * Creates a read receipt DTO with custom timestamp
     */
    public ReadReceiptDto createReadReceiptDto(ConversationParticipant participant, Message lastReadMessage, Instant readAt) {
        ReadReceiptDto receipt = createReadReceiptDto(participant, lastReadMessage);
        receipt.setReadAt(readAt);
        return receipt;
    }
    
    /**
     * Converts a list of participants to read receipt DTOs
     */
    public List<ReadReceiptDto> toReadReceiptDtoList(List<ConversationParticipant> participants) {
        if (participants == null || participants.isEmpty()) {
            return List.of();
        }
        return readReceiptMapper.toReadReceiptDtoList(participants);
    }
    
    /**
     * Creates a batch of read receipts for multiple participants
     */
    public List<ReadReceiptDto> createBatchReadReceipts(List<ConversationParticipant> participants, Message lastReadMessage) {
        if (participants == null || participants.isEmpty() || lastReadMessage == null) {
            return List.of();
        }
        
        return participants.stream()
            .map(participant -> createReadReceiptDto(participant, lastReadMessage))
            .toList();
    }

    /**
     * Creates a read receipt DTO with minimal information for real-time broadcasting
     */
    public ReadReceiptDto createBroadcastReadReceipt(ConversationParticipant participant, Message lastReadMessage) {
        if (participant == null || lastReadMessage == null) {
            return null;
        }
        
        ReadReceiptDto receipt = new ReadReceiptDto();
        receipt.setUserId(participant.getUser().getUserId());
        receipt.setUsername(participant.getUser().getUsername());
        receipt.setProfilePhotoUrl(participant.getUser().getProfilePhotoUrl());
        receipt.setConversationId(participant.getConversation().getConversationId());
        receipt.setLastReadTimestamp(participant.getLastReadTimestamp());
        receipt.setLastReadMessageId(lastReadMessage.getId());
        receipt.setReadAt(Instant.now());
        
        return receipt;
    }
}