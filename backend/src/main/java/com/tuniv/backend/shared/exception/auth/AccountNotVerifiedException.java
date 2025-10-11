package com.tuniv.backend.shared.exception.auth;

public class AccountNotVerifiedException extends RuntimeException {
    public AccountNotVerifiedException(String message) { super(message); }
}