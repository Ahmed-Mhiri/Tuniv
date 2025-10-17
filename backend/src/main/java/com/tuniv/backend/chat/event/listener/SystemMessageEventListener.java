package com.tuniv.backend.chat.event.listener;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tuniv.backend.chat.event.SystemMessageRequestedEvent;
import com.tuniv.backend.chat.service.MessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event listener for handling system message requests asynchronously with retry capabilities.
 * Processes system message events after transaction commit to ensure data consistency.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemMessageEventListener {

    private final MessageService messageService;

    /**
     * Handles system message requested events asynchronously with retry mechanism.
     * Executes after the transaction commits to ensure data consistency.
     *
     * @param event the system message requested event
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void handleSystemMessageRequested(SystemMessageRequestedEvent event) {
        try {
            log.debug("Processing system message event for conversation {}: {}", 
                     event.getConversationId(), event.getMessageText());
            
            messageService.createAndSendSystemMessage(
                event.getConversationId(),
                event.getMessageText()
            );
            
            log.debug("Successfully processed system message event for conversation {}", 
                     event.getConversationId());
        } catch (Exception e) {
            log.error("Failed to process system message event for conversation {}: {}", 
                     event.getConversationId(), e.getMessage(), e);
            throw e; // Re-throw for retry mechanism
        }
    }

    /**
     * Recovery method when all retry attempts have failed.
     * Implements fallback logic for handling permanent failures.
     *
     * @param event the system message requested event that failed
     * @param e the exception that caused the failure
     */
    @Recover
    public void recover(SystemMessageRequestedEvent event, Exception e) {
        log.error("All retry attempts failed for system message event for conversation {}: {}", 
                 event.getConversationId(), e.getMessage());
        // Implement fallback logic or send to dead letter queue
        // Example fallback: log to database, send alert, or queue for manual processing
    }
}
