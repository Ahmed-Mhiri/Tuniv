package com.tuniv.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange for chat messages
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange("chat.exchange");
    }

    // ✅ ADDED: Queue for the last message updater worker
    @Bean
    public Queue conversationLastMessageQueue() {
        return new Queue("q.conversation.last_message_updater", true); 
    }
    
    // ✅ ADDED: Binding for the new queue to the chat exchange
    @Bean
    public Binding conversationLastMessageBinding(Queue conversationLastMessageQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(conversationLastMessageQueue)
                .to(chatExchange)
                .with("event.message.new"); 
    }

    // Queue for conversation-specific messages
    @Bean
    public Queue conversationQueue() {
        return new Queue("chat.conversation.queue", true);
    }

    // Queue for user-specific notifications
    @Bean
    public Queue userNotificationsQueue() {
        return new Queue("chat.user.notifications.queue", true);
    }

    // Binding for conversation topics
    @Bean
    public Binding conversationBinding(Queue conversationQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(conversationQueue)
                .to(chatExchange)
                .with("conversation.*");
    }

    // Binding for user notifications
    @Bean
    public Binding userNotificationsBinding(Queue userNotificationsQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(userNotificationsQueue)
                .to(chatExchange)
                .with("user.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}