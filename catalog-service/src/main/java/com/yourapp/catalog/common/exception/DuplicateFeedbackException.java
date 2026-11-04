package com.yourapp.catalog.common.exception;

public class DuplicateFeedbackException extends RuntimeException {
    public DuplicateFeedbackException(String message) {
        super(message);
    }
}
