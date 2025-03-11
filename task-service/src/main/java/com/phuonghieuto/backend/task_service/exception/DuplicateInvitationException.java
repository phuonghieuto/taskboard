package com.phuonghieuto.backend.task_service.exception;

public class DuplicateInvitationException extends RuntimeException {
    public DuplicateInvitationException(String message) {
        super(message);
    }
}