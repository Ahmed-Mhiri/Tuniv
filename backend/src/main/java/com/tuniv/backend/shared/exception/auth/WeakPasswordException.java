package com.tuniv.backend.shared.exception.auth;

public class WeakPasswordException extends RuntimeException {
    public WeakPasswordException(String message) { super(message); }
}