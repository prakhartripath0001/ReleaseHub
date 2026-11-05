package com.releasehub.feedback.feedback.service;

public class DuplicateFeedbackException extends RuntimeException {

    public DuplicateFeedbackException(String message) {
        super(message);
    }

    public DuplicateFeedbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
