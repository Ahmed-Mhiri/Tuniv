package com.tuniv.backend.chat.aspect;

import java.lang.reflect.Parameter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.tuniv.backend.chat.annotation.RequiresMembership;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

        // 2. Extract conversationId using only the explicit parameter name
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Integer conversationId = extractConversationId(joinPoint.getArgs(), signature.getMethod().getParameters(), requiresMembership.conversationIdParam());
        if (conversationId == null) {
            // This now correctly fires if the explicit parameter isn't found
            throw new IllegalArgumentException("Conversation ID could not be determined for membership check. " +
                    "Ensure @RequiresMembership points to a valid Integer parameter named '" + requiresMembership.conversationIdParam() + "'.");
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
            
            // Check if this is the conversationId parameter by name
            if (param.getName().equals(conversationIdParamName) && args[i] instanceof Integer) {
                return (Integer) args[i];
            }
            
            // Also check for @DestinationVariable annotation with matching name
            DestinationVariable destVar = param.getAnnotation(DestinationVariable.class);
            if (destVar != null && (param.getName().equals(conversationIdParamName) || 
                destVar.value().equals(conversationIdParamName)) && args[i] instanceof Integer) {
                return (Integer) args[i];
            }
        }
        
        // ✅ FIX: Completely removed the fragile fallback logic
        // The aspect now relies exclusively on the explicitly defined conversationIdParam
        return null;
    }
}