package com.phuonghieuto.backend.task_service.exception.exception_handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.phuonghieuto.backend.task_service.exception.BoardNotFoundException;
import com.phuonghieuto.backend.task_service.exception.TableNotFoundException;
import com.phuonghieuto.backend.task_service.exception.TaskNotFoundException;
import com.phuonghieuto.backend.task_service.exception.TokenAlreadyInvalidatedException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.common.CustomError;

import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler named {@link GlobalExceptionHandler} for handling
 * various types of exceptions in the application.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        /**
         * Handles MethodArgumentNotValidException thrown when validation on an argument
         * annotated with @Valid fails.
         *
         * @param ex The MethodArgumentNotValidException instance.
         * @return ResponseEntity with CustomError containing details of validation
         *         errors.
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex) {
                log.error("Validation error: {}", ex.getMessage());
                
                List<CustomError.CustomSubError> subErrors = new ArrayList<>();

                ex.getBindingResult().getAllErrors().forEach(error -> {
                        String fieldName = ((FieldError) error).getField();
                        String message = error.getDefaultMessage();
                        subErrors.add(CustomError.CustomSubError.builder().field(fieldName).message(message).build());
                        log.debug("Validation field error: {} - {}", fieldName, message);
                });

                CustomError customError = CustomError.builder().httpStatus(HttpStatus.BAD_REQUEST)
                                .header(CustomError.Header.VALIDATION_ERROR.getName()).message("Validation failed")
                                .subErrors(subErrors).build();

                return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handles ConstraintViolationException thrown when a bean validation constraint
         * is violated.
         *
         * @param constraintViolationException The ConstraintViolationException
         *                                     instance.
         * @return ResponseEntity with CustomError containing details of constraint
         *         violations.
         */
        @ExceptionHandler(ConstraintViolationException.class)
        protected ResponseEntity<Object> handlePathVariableErrors(
                        final ConstraintViolationException constraintViolationException) {
                log.error("Constraint violation error: {}", constraintViolationException.getMessage());

                List<CustomError.CustomSubError> subErrors = new ArrayList<>();
                constraintViolationException.getConstraintViolations()
                                .forEach(constraintViolation -> {
                                    String field = StringUtils.substringAfterLast(
                                            constraintViolation.getPropertyPath().toString(), ".");
                                    String message = constraintViolation.getMessage();
                                    
                                    subErrors.add(CustomError.CustomSubError.builder()
                                                .message(message)
                                                .field(field)
                                                .value(constraintViolation.getInvalidValue() != null
                                                                ? constraintViolation.getInvalidValue().toString()
                                                                : null)
                                                .type(constraintViolation.getInvalidValue() != null
                                                                ? constraintViolation.getInvalidValue().getClass().getSimpleName()
                                                                : "null")
                                                .build());
                                    
                                    log.debug("Constraint violation: field={}, message={}, value={}",
                                            field, message, constraintViolation.getInvalidValue());
                                });

                CustomError customError = CustomError.builder().httpStatus(HttpStatus.BAD_REQUEST)
                                .header(CustomError.Header.VALIDATION_ERROR.getName()).message("Constraint violation")
                                .subErrors(subErrors).build();

                return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
        }

        /**
         * Handles RuntimeException thrown for general runtime exceptions.
         *
         * @param runtimeException The RuntimeException instance.
         * @return ResponseEntity with CustomError containing details of the runtime
         *         exception.
         */
        @ExceptionHandler(RuntimeException.class)
        protected ResponseEntity<?> handleRuntimeException(final RuntimeException runtimeException) {
                log.error("Runtime exception occurred: {}", runtimeException.getMessage(), runtimeException);
                
                CustomError customError = CustomError.builder().httpStatus(HttpStatus.NOT_FOUND)
                                .header(CustomError.Header.API_ERROR.getName()).message(runtimeException.getMessage())
                                .build();

                return new ResponseEntity<>(customError, HttpStatus.NOT_FOUND);
        }

        /**
         * Handles TokenAlreadyInvalidatedException thrown when a token is already
         * invalidated.
         *
         * @param ex The TokenAlreadyInvalidatedException instance.
         * @return ResponseEntity with CustomError containing details of the exception.
         */
        @ExceptionHandler(TokenAlreadyInvalidatedException.class)
        protected ResponseEntity<Object> handleTokenAlreadyInvalidatedException(
                        final TokenAlreadyInvalidatedException ex) {
                log.warn("Token already invalidated: {}", ex.getMessage());
                
                CustomError customError = CustomError.builder().httpStatus(HttpStatus.BAD_REQUEST)
                                .header(CustomError.Header.API_ERROR.getName()).message(ex.getMessage()).build();

                return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(BoardNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public CustomError handleBoardNotFoundException(BoardNotFoundException ex) {
                log.error("Board not found: {}", ex.getMessage());
                
                return CustomError.builder()
                                .header(CustomError.Header.BOARD_NOT_FOUND.getName())
                                .httpStatus(HttpStatus.NOT_FOUND)
                                .isSuccess(false)
                                .message(ex.getMessage())
                                .build();
        }

        @ExceptionHandler(UnauthorizedAccessException.class)
        @ResponseStatus(HttpStatus.FORBIDDEN)
        public CustomError handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
                log.warn("Unauthorized access attempt: {}", ex.getMessage());
                
                return CustomError.builder()
                                .header(CustomError.Header.UNAUTHORIZED_ACCESS.getName())
                                .httpStatus(HttpStatus.FORBIDDEN)
                                .isSuccess(false)
                                .message(ex.getMessage())
                                .build();
        }

        @ExceptionHandler(TableNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public CustomError handleTableNotFoundException(TableNotFoundException ex) {
            log.error("Table not found: {}", ex.getMessage());
            
            return CustomError.builder()
                    .header(CustomError.Header.TABLE_NOT_FOUND.getName())
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .isSuccess(false)
                    .message(ex.getMessage())
                    .build();
        }
        
        @ExceptionHandler(TaskNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public CustomError handleTaskNotFoundException(TaskNotFoundException ex) {
            log.error("Task not found: {}", ex.getMessage());
            
            return CustomError.builder()
                    .header(CustomError.Header.TASK_NOT_FOUND.getName())
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .isSuccess(false)
                    .message(ex.getMessage())
                    .build();
        }
}