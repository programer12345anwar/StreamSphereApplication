package com.streamsphere.central.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return new ResponseEntity<>(Map.of("error", message.isBlank() ? "Validation failed" : message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String message = "Database constraint violation. The email or phone number might already be registered.";
        if (ex.getMostSpecificCause() != null) {
            String specMessage = ex.getMostSpecificCause().getMessage();
            if (specMessage != null) {
                if (specMessage.contains("users_email_key") || specMessage.contains("email")) {
                    message = "Email address is already registered.";
                } else if (specMessage.contains("users_phone_number_key") || specMessage.contains("phone_number")) {
                    message = "Phone number is already registered.";
                }
            }
        }
        return new ResponseEntity<>(Map.of("error", message), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return new ResponseEntity<>(Map.of("error", "Access Denied"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UserNotFound.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFound ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ChannelNotFound.class)
    public ResponseEntity<Map<String, String>> handleChannelNotFound(ChannelNotFound ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return new ResponseEntity<>(Map.of("error", "An unexpected error occurred"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

