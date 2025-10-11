package com.tuniv.backend.auth.validation;

import org.springframework.stereotype.Component;

import com.tuniv.backend.shared.exception.auth.WeakPasswordException;

@Component
public class PasswordValidator {
    
    public void validate(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new WeakPasswordException("Password cannot be empty");
        }
        
        if (password.length() < 8) {
            throw new WeakPasswordException("Password must be at least 8 characters");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new WeakPasswordException("Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*\\d.*")) {
            throw new WeakPasswordException("Password must contain at least one number");
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new WeakPasswordException("Password must contain at least one special character");
        }
        
        // Check for common passwords (simplified example)
        if (isCommonPassword(password)) {
            throw new WeakPasswordException("Password is too common. Please choose a more unique password.");
        }
    }
    
    private boolean isCommonPassword(String password) {
        String[] commonPasswords = {
            "password", "123456", "12345678", "123456789", "qwerty",
            "abc123", "password1", "admin", "welcome", "monkey"
        };
        
        for (String common : commonPasswords) {
            if (password.equalsIgnoreCase(common)) {
                return true;
            }
        }
        return false;
    }
}