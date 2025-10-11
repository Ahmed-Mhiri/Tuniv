package com.tuniv.backend.shared.exception.auth;

public class InvalidTwoFactorCodeException extends RuntimeException {
    public InvalidTwoFactorCodeException(String message) { super(message); }
}