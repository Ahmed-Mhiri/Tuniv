package com.tuniv.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.tuniv.backend.config.security.jwt.JwtAuthChannelInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final JwtAuthChannelInterceptor jwtAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Keep SockJS for fallback
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Configure RabbitMQ as the external message broker
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost("localhost") // Use your RabbitMQ host
                .setRelayPort(61613) // STOMP port for RabbitMQ
                .setClientLogin("guest") // Default RabbitMQ credentials
                .setClientPasscode("guest")
                .setSystemLogin("guest")
                .setSystemPasscode("guest")
                .setVirtualHost("/");
        
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}