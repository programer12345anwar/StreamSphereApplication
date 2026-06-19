package com.youtube.central.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

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
        return new ResponseEntity<>(Map.of("error", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
