package com.tuniv.backend.chat.aspect;

import com.tuniv.backend.chat.annotation.RequiresMembership;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationMembershipAspect {

    private final ConversationParticipantRepository participantRepository;

    @Around("@annotation(requiresMembership)")
    public Object validateMembership(ProceedingJoinPoint joinPoint, RequiresMembership requiresMembership) throws Throwable {
        // 1. Get UserDetails directly from SecurityContext (more reliable)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new AccessDeniedException("User authentication required");
        }
        UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();

        // 2. Extract conversationId (your existing logic is good here)
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Integer conversationId = extractConversationId(joinPoint.getArgs(), signature.getMethod().getParameters(), requiresMembership.conversationIdParam());
        if (conversationId == null) {
            throw new IllegalArgumentException("Conversation ID could not be determined for membership check");
        }

        // 3. Perform a direct, efficient membership check
        boolean isMember = participantRepository.isUserActiveParticipant(conversationId, currentUser.getId());
        if (!isMember) {
            throw new AccessDeniedException("Not an active member of this conversation");
        }

        log.debug("AOP: Membership validated for user {} in conversation {}", currentUser.getId(), conversationId);
        return joinPoint.proceed();
    }

    private Integer extractConversationId(Object[] args, Parameter[] parameters, String conversationIdParamName) {
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            
            // Check if this is the conversationId parameter
            if (param.getName().equals(conversationIdParamName) && args[i] instanceof Integer) {
                return (Integer) args[i];
            }
            
            // Also check for @DestinationVariable annotation
            DestinationVariable destVar = param.getAnnotation(DestinationVariable.class);
            if (destVar != null && (param.getName().equals(conversationIdParamName) || 
                destVar.value().equals(conversationIdParamName)) && args[i] instanceof Integer) {
                return (Integer) args[i];
            }
        }
        
        // Fallback: look for any Integer parameter with "conversation" in name
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName().toLowerCase();
            if (paramName.contains("conversation") && args[i] instanceof Integer) {
                return (Integer) args[i];
            }
        }
        
        return null;
    }
}