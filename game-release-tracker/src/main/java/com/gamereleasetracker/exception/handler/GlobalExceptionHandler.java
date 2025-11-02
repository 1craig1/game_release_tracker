package com.gamereleasetracker.exception.handler;

import com.gamereleasetracker.exception.InvalidPasswordException;
import com.gamereleasetracker.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler to process exceptions into standard HTTP responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles NotFoundException and returns a 404 Not Found response.
     *
     * @param ex      The caught NotFoundException.
     * @param request The current web request.
     * @return A ResponseEntity with a 404 status and a structured error body.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles InvalidPasswordException and returns a 400 Bad Request response.
     * 
     * @param ex The caught InvalidPasswordException.
     * @return A ResponseEntity with a 400 status and a simple error message.
     */
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException ex) {
        // This sends a clean JSON: {"message": "Incorrect current password."}
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(ex.getMessage()));
    }

    record ErrorResponse(String message) {}
}