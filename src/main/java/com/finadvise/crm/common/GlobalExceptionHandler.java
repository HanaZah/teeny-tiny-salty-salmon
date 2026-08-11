package com.finadvise.crm.common;

import com.finadvise.crm.addresses.AddressValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid employee ID or password."
        );
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setType(URI.create(ErrorCodes.BAD_CREDENTIALS));

        return problemDetail;
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail handleAccessDeniedException() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource."
        );
        problemDetail.setTitle("Access Denied");
        problemDetail.setType(URI.create(ErrorCodes.ACCESS_DENIED));
        return problemDetail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                e.getMessage()
        );
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create(ErrorCodes.RESOURCE_NOT_FOUND));

        return problemDetail;
    }

    @ExceptionHandler(SystemIntegrityException.class)
    public ProblemDetail handleSystemIntegrityException() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "A critical system failure has occurred."
        );

        problemDetail.setTitle("Critical System Failure");
        problemDetail.setType(URI.create(ErrorCodes.SYSTEM_FAILURE));

        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "The request payload contains validation errors."
        );
        problemDetail.setTitle("Validation Failed");
        problemDetail.setType(URI.create(ErrorCodes.VALIDATION_FAILED));

        // Extract all field errors into a Map of "fieldName": "errorMessage"
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing // If multiple errors exist for one field, keep the first one
                ));

        problemDetail.setProperty("invalid_params", fieldErrors);

        return problemDetail;
    }

    @ExceptionHandler({ResourceVersionMismatchException.class, ObjectOptimisticLockingFailureException.class})
    public ProblemDetail handleVersionMismatch() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The record was modified by another transaction. Please refresh and retry."
        );
        problemDetail.setTitle("Version Mismatch");
        problemDetail.setType(URI.create(ErrorCodes.VERSION_MISMATCH));

        return problemDetail;
    }

    @ExceptionHandler({InvalidInputValueException.class, AddressValidationException.class})
    public ProblemDetail handleInvalidInputValue(Exception e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                e.getMessage()
        );

        problemDetail.setTitle("Invalid Input Value");
        problemDetail.setType(URI.create(ErrorCodes.INVALID_INPUT_VALUE));

        return problemDetail;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed. Please provide a valid token."
        );
        problemDetail.setTitle("Unauthorized");
        problemDetail.setType(URI.create(ErrorCodes.UNAUTHORIZED));

        return problemDetail;
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ProblemDetail handleResourceConflictException(ResourceConflictException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                e.getMessage()
        );
        problemDetail.setTitle("Resource Conflict");
        problemDetail.setType(URI.create(ErrorCodes.RESOURCE_CONFLICT));

        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "The request parameters contain validation errors."
        );
        problemDetail.setTitle("Validation Failed");
        problemDetail.setType(URI.create(ErrorCodes.VALIDATION_FAILED));

        Map<String, String> constraintErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        v -> {
                            String path = v.getPropertyPath().toString();
                            // Strip method name prefix if present (e.g. "getRecentClientOverviews.limit" -> "limit")
                            int lastDot = path.lastIndexOf('.');
                            return lastDot != -1 ? path.substring(lastDot + 1) : path;
                        },
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        problemDetail.setProperty("invalid_params", constraintErrors);

        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadableException() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request body."
        );
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create(ErrorCodes.VALIDATION_FAILED));

        return problemDetail;
    }
}
