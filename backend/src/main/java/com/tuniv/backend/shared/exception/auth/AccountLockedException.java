package com.tuniv.backend.shared.exception.auth;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) { super(message); }
}