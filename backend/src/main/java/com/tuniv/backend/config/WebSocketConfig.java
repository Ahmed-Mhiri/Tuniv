package com.tuniv.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Defines message prefixes for topics that clients can subscribe to.
        // E.g., a client would subscribe to "/topic/conversation/123"
        registry.enableSimpleBroker("/topic");
        
        // Defines the prefix for messages bound for @MessageMapping-annotated methods.
        // E.g., a client would send a message to "/app/chat.sendMessage"
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The endpoint that clients will connect to for the WebSocket handshake.
        // withSockJS() provides a fallback for browsers that don't support WebSockets.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}