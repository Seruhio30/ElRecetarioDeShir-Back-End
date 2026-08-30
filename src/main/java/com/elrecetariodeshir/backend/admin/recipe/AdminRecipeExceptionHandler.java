package com.elrecetariodeshir.backend.admin.recipe;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = AdminRecipeController.class)
public class AdminRecipeExceptionHandler {

    @ExceptionHandler({
            AdminRecipeValidationException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<AdminRecipeErrorResponse> handleBadRequest(
            Exception exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new AdminRecipeErrorResponse(
                        "BAD_REQUEST",
                        "Invalid admin recipe request."));
    }

    @ExceptionHandler(AdminRecipeNotFoundException.class)
    public ResponseEntity<AdminRecipeErrorResponse> handleNotFound(
            AdminRecipeNotFoundException exception) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new AdminRecipeErrorResponse(
                        "NOT_FOUND",
                        "Recipe not found."));
    }

    @ExceptionHandler({
            AdminRecipeConflictException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<AdminRecipeErrorResponse> handleConflict(
            Exception exception) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new AdminRecipeErrorResponse(
                        "CONFLICT",
                        "Recipe operation conflicts with current state."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AdminRecipeErrorResponse> handleUnexpected(
            Exception exception) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AdminRecipeErrorResponse(
                        "INTERNAL_SERVER_ERROR",
                        "Unable to process admin recipe request."));
    }
}
