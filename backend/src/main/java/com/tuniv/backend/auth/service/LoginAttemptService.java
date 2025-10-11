package com.tuniv.backend.auth.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LoginAttemptService {
    
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION = 15 * 60 * 1000; // 15 minutes
    
    private final Map<String, LoginAttempt> attemptsCache = new ConcurrentHashMap<>();
    
    public void loginFailed(String username) {
        LoginAttempt attempt = attemptsCache.getOrDefault(username, new LoginAttempt());
        attempt.incrementAttempts();
        attempt.setLastAttempt(Instant.now());
        attemptsCache.put(username, attempt);
        
        log.warn("Login failed for user: {}. Attempt: {}", username, attempt.getAttempts());
        
        if (attempt.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("Account locked for user: {} due to too many failed attempts", username);
        }
    }
    
    public void loginSuccess(String username) {
        attemptsCache.remove(username);
        log.debug("Login success for user: {}, attempts cache cleared", username);
    }
    
    public boolean isBlocked(String username) {
        LoginAttempt attempt = attemptsCache.get(username);
        if (attempt == null) {
            return false;
        }
        
        if (attempt.getAttempts() < MAX_ATTEMPTS) {
            return false;
        }
        
        // Check if lock time has expired
        if (Instant.now().isAfter(attempt.getLastAttempt().plusMillis(LOCK_TIME_DURATION))) {
            attemptsCache.remove(username);
            log.info("Lock period expired for user: {}", username);
            return false;
        }
        
        return true;
    }
    
    public int getRemainingAttempts(String username) {
        LoginAttempt attempt = attemptsCache.get(username);
        if (attempt == null) {
            return MAX_ATTEMPTS;
        }
        return Math.max(0, MAX_ATTEMPTS - attempt.getAttempts());
    }
    
    @Scheduled(fixedRate = 3600000) // Cleanup every hour
    public void cleanupExpiredLocks() {
        Instant cutoff = Instant.now().minusMillis(LOCK_TIME_DURATION);
        attemptsCache.entrySet().removeIf(entry -> 
            entry.getValue().getLastAttempt().isBefore(cutoff)
        );
        log.debug("Cleaned up expired login attempt locks");
    }
    
    private static class LoginAttempt {
        private int attempts;
        private Instant lastAttempt;
        
        public LoginAttempt() {
            this.attempts = 0;
            this.lastAttempt = Instant.now();
        }
        
        public void incrementAttempts() {
            this.attempts++;
            this.lastAttempt = Instant.now();
        }
        
        public int getAttempts() { return attempts; }
        public Instant getLastAttempt() { return lastAttempt; }
        public void setLastAttempt(Instant lastAttempt) { this.lastAttempt = lastAttempt; }
    }
}