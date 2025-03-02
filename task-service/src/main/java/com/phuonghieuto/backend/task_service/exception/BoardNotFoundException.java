// BoardNotFoundException.java
package com.phuonghieuto.backend.task_service.exception;

public class BoardNotFoundException extends RuntimeException {
    public BoardNotFoundException(String message) {
        super(message);
    }
}