package com.tuniv.backend.shared.exception.auth;

public class InvalidVerificationTokenException extends RuntimeException {
    public InvalidVerificationTokenException(String message) { super(message); }
}