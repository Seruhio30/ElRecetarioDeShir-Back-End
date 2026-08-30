package com.elrecetariodeshir.backend.admin.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminAuthController.class)
public class AdminAuthExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<AdminAuthErrorResponse> handleAuthentication(
            AuthenticationException exception) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new AdminAuthErrorResponse(
                        "ADMIN_AUTHENTICATION_FAILED",
                        "Invalid admin credentials."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AdminAuthErrorResponse> handleValidation(
            MethodArgumentNotValidException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new AdminAuthErrorResponse(
                        "BAD_REQUEST",
                        "Invalid admin authentication request."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AdminAuthErrorResponse> handleUnexpected(
            Exception exception) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AdminAuthErrorResponse(
                        "INTERNAL_SERVER_ERROR",
                        "Unable to process admin authentication request."));
    }
}
