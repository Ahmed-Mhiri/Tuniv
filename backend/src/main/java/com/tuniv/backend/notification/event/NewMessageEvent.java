package com.tuniv.backend.notification.event;

import java.util.List;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.user.model.User;

import lombok.Getter;

@Getter
public class NewMessageEvent extends ApplicationEvent {

    private final Message message;
    private final List<User> recipients; // ✅ Add this field

    /**
     * ✅ Update the constructor to accept the list of recipients.
     * @param source The component that published the event (e.g., ChatService).
     * @param message The new message that was sent.
     * @param recipients The list of users who should be notified.
     */
    public NewMessageEvent(Object source, Message message, List<User> recipients) {
        super(source);
        this.message = message;
        this.recipients = recipients;
    }
}
