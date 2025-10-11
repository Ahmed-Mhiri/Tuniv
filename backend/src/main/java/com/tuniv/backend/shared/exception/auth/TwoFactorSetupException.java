package com.tuniv.backend.shared.exception.auth;

public class TwoFactorSetupException extends RuntimeException {
    public TwoFactorSetupException(String message) {
        super(message);
    }

    public TwoFactorSetupException(String message, Throwable cause) {
        super(message, cause);
    }
}
