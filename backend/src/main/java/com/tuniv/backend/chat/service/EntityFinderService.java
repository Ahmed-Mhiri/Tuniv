package com.tuniv.backend.chat.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.ConversationRole;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.repository.ConversationRoleRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
import com.tuniv.backend.chat.repository.ReactionRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntityFinderService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final ConversationRoleRepository conversationRoleRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;

    public Conversation getConversationOrThrow(Integer id) {
        return conversationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));
    }

    public Message getMessageOrThrow(Integer id) {
        return messageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + id));
    }

    public Reaction getReactionOrThrow(Integer id) {
        return reactionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reaction not found with id: " + id));
    }

    public User getUserOrThrow(Integer id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public ConversationRole getConversationRoleOrThrow(Integer id) {
        return conversationRoleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation role not found with id: " + id));
    }

    public List<User> getUsersByIds(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findByUserIdIn(userIds);
    }

    public List<Conversation> getConversationsByIds(List<Integer> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return List.of();
        }
        return conversationRepository.findByConversationIdIn(conversationIds);
    }

    public Conversation getConversationWithParticipantsOrThrow(Integer id) {
        return conversationRepository.findWithParticipantsById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));
    }

    public Message getMessageWithConversationOrThrow(Integer id) {
        return messageRepository.findWithConversationById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + id));
    }

    public boolean userExists(Integer userId) {
        return userRepository.existsById(userId);
    }

    public boolean conversationExists(Integer conversationId) {
        return conversationRepository.existsById(conversationId);
    }

    public boolean messageExists(Integer messageId) {
        return messageRepository.existsById(messageId);
    }

    // Conversation Participant methods
    public ConversationParticipant getConversationParticipantOrThrow(Integer conversationId, Integer userId) {
        return conversationParticipantRepository.findByConversation_ConversationIdAndUser_UserId(conversationId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Conversation participant not found for conversationId: " + conversationId + " and userId: " + userId));
    }

    public ConversationParticipant getActiveConversationParticipantOrThrow(Integer conversationId, Integer userId) {
        return conversationParticipantRepository.findActiveParticipant(conversationId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Active conversation participant not found for conversationId: " + conversationId + " and userId: " + userId));
    }

    public Optional<ConversationParticipant> getConversationParticipant(Integer conversationId, Integer userId) {
        return conversationParticipantRepository.findByConversation_ConversationIdAndUser_UserId(conversationId, userId);
    }

    public Optional<ConversationParticipant> getActiveConversationParticipant(Integer conversationId, Integer userId) {
        return conversationParticipantRepository.findActiveParticipant(conversationId, userId);
    }

    public List<ConversationParticipant> getActiveParticipantsByConversation(Integer conversationId) {
        return conversationParticipantRepository.findActiveParticipantsByConversationId(conversationId);
    }

    public List<ConversationParticipant> getAllParticipantsByConversation(Integer conversationId) {
        return conversationParticipantRepository.findByConversation_ConversationId(conversationId);
    }

    public List<ConversationParticipant> getActiveConversationsByUser(Integer userId) {
        return conversationParticipantRepository.findActiveConversationsByUserId(userId);
    }

    public boolean isUserActiveParticipant(Integer conversationId, Integer userId) {
        return conversationParticipantRepository.findActiveParticipant(conversationId, userId).isPresent();
    }

    public boolean isUserParticipant(Integer conversationId, Integer userId) {
        return conversationParticipantRepository.findByConversation_ConversationIdAndUser_UserId(conversationId, userId).isPresent();
    }
}