// UnauthorizedAccessException.java
package com.phuonghieuto.backend.task_service.exception;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}