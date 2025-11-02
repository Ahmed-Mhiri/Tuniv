package com.tuniv.backend.chat.event;

import java.time.Instant;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class MessageReadEvent extends ApplicationEvent {

    private final Integer messageId;
    private final Integer userId;
    // Rename this field to avoid conflict with ApplicationEvent.getTimestamp()
    private final Instant eventTimestamp; // ✨ Renamed

    /**
     * Create a new MessageReadEvent.
     * @param source the object on which the event initially occurred (never {@code null})
     * @param messageId The ID of the message marked as read.
     * @param userId The ID of the user who read the message.
     */
    public MessageReadEvent(Object source, Integer messageId, Integer userId) {
        super(source); // Calls the super constructor, which sets the original timestamp (long)
        this.messageId = messageId;
        this.userId = userId;
        this.eventTimestamp = Instant.now(); // ✨ Set the renamed field
    }

    // You can still access the original timestamp (long) using super.getTimestamp() if needed
    public long getOriginalTimestampMillis() {
        return super.getTimestamp();
    }

    // Optional: Update toString if you had one
    @Override
    public String toString() {
        return "MessageReadEvent{" +
                "messageId=" + messageId +
                ", userId=" + userId +
                ", eventTimestamp=" + eventTimestamp + // ✨ Updated field name
                ", source=" + source +
                ", originalTimestampMillis=" + getOriginalTimestampMillis() + // Show original too
                '}';
    }
}