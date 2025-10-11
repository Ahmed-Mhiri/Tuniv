package com.tuniv.backend.shared.exception.auth;


public class TwoFactorVerificationException extends RuntimeException {
    public TwoFactorVerificationException(String message) { super(message); }
}