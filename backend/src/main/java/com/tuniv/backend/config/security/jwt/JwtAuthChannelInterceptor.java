package com.tuniv.backend.config.security.jwt;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.tuniv.backend.config.security.services.UserDetailsServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil; // Use your JwtUtil class
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        // Check if the message is a CONNECT frame from the client
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Get the "Authorization" header from the STOMP connect headers
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                
                // Validate the token using your JwtUtil
                if (jwtUtil.validateJwtToken(jwt)) {
                    // Get username from the token
                    String username = jwtUtil.getUsernameFromJwtToken(jwt);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    // Create an authentication object
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    
                    // Set the authenticated user for the current WebSocket session
                    accessor.setUser(authentication);
                    log.info("Authenticated WebSocket user: {}", username);
                }
            }
        }
        return message;
    }
}