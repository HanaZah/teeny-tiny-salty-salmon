package com.finadvise.crm.common;

import com.finadvise.crm.addresses.AddressValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    private String resolveMessage(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            log.warn("Missing localization for error code: {}", code);
            return code; // Fallback to raw code if mapping is missing
        }
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException() {
        String errorCode = "error.auth.bad-credentials";
        log.warn("Authentication failed: {}", errorCode);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                resolveMessage(errorCode)
        );
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setType(URI.create(ErrorCodes.BAD_CREDENTIALS));
        problemDetail.setProperty("errorCode", errorCode);

        return problemDetail;
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail handleAccessDeniedException() {
        String errorCode = "error.auth.access-denied";
        log.warn("Authorization denied: {}", errorCode);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                resolveMessage(errorCode)
        );
        problemDetail.setTitle("Access Denied");
        problemDetail.setType(URI.create(ErrorCodes.ACCESS_DENIED));
        problemDetail.setProperty("errorCode", errorCode);

        return problemDetail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException e) {
        String errorCode = e.getMessage();
        log.warn("Resource not found: {}", errorCode);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                resolveMessage(errorCode)
        );
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create(ErrorCodes.RESOURCE_NOT_FOUND));
        problemDetail.setProperty("errorCode", errorCode);

        return problemDetail;
    }

    @ExceptionHandler(SystemIntegrityException.class)
    public ProblemDetail handleSystemIntegrityException(SystemIntegrityException e) {
        String errorCode = "error.system.critical-failure";
        log.error("System integrity failure. Details: {}", e.getMessage(), e);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                resolveMessage(errorCode)
        );
        problemDetail.setTitle("Critical System Failure");
        problemDetail.setType(URI.create(ErrorCodes.SYSTEM_FAILURE));
        problemDetail.setProperty("errorCode", errorCode);

        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        String errorCode = "error.validation.payload";
        log.warn("Payload validation failed: {}", errorCode);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                resolveMessage(errorCode)
        );
        problemDetail.setTitle("Validation Failed");
        problemDetail.setType(URI.create(ErrorCodes.VALIDATION_FAILED));
        problemDetail.setProperty("errorCode", errorCode);

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? resolveMessage(error.getDefaultMessage()) : resolveMessage("error.validation.default"),
                        (existing, replacement) -> existing
                ));

        problemDetail.setProperty("invalid_params", fieldErrors);

        return problemDetail;
    }

    @ExceptionHandler({ResourceVersionMismatchException.class, ObjectOptimisticLockingFailureException.class})
    public ProblemDetail handleVersionMismatch() {
        String errorCode = "error.concurrency.version-mismatch";
        log.warn("Concurrency conflict: {}", errorCode);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                resolveMessage(errorCode)
        );
        problemDetail.setTitle("Version Mismatch");
        problemDetail.setType(URI.create(ErrorCodes.VERSION_MISMATCH));
        problemDetail.setProperty("errorCode", errorCode);

        return problemDetail;
    }

    @ExceptionHandler({InvalidInputValueException.class, AddressValidationException.class})
    public ProblemDetail handleInvalidInputValue(Exception e) {
        String errorCode = e.getMessage();
        log.warn("Invalid input value: {}", errorCode);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                resolveMessage(errorCode)
        );
        problemDetail.setTitle("Invalid Input Value");
        problemDetail.setType(URI.create(ErrorCodes.INVALID_INPUT_VALUE));
        problemDetail.setProperty("errorCode", errorCode);

        return problemDetail;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException() {
        String errorCode = "error.auth.unauthorized";
        log.warn("Authentication required: {}", errorCode);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                resolveMessage(errorCode)
        );
        problemDetail.setTitle("Unauthorized");
        problemDetail.setType(URI.create(ErrorCodes.UNAUTHORIZED));
        problemDetail.setProperty("errorCode", errorCode);

        return problemDetail;
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ProblemDetail handleResourceConflictException(ResourceConflictException e) {
        String errorCode = e.getMessage();
        log.warn("Resource conflict: {}", errorCode);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                resolveMessage(errorCode)
        );
        problemDetail.setTitle("Resource Conflict");
        problemDetail.setType(URI.create(ErrorCodes.RESOURCE_CONFLICT));
        problemDetail.setProperty("errorCode", errorCode);

        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex) {
        String errorCode = "error.validation.params";
        log.warn("Parameter validation failed: {}", errorCode);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                resolveMessage(errorCode)
        );
        problemDetail.setTitle("Validation Failed");
        problemDetail.setType(URI.create(ErrorCodes.VALIDATION_FAILED));
        problemDetail.setProperty("errorCode", errorCode);

        Map<String, String> constraintErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        v -> {
                            String path = v.getPropertyPath().toString();
                            int lastDot = path.lastIndexOf('.');
                            return lastDot != -1 ? path.substring(lastDot + 1) : path;
                        },
                        v -> resolveMessage(v.getMessage()),
                        (existing, replacement) -> existing
                ));

        problemDetail.setProperty("invalid_params", constraintErrors);

        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadableException() {
        String errorCode = "error.http.malformed-request";
        log.warn("Malformed JSON request: {}", errorCode);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                resolveMessage(errorCode)
        );
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create(ErrorCodes.VALIDATION_FAILED));
        problemDetail.setProperty("errorCode", errorCode);

        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String errorCode = "error.http.type-mismatch";
        log.warn("Type mismatch for parameter {}: {}", ex.getName(), errorCode);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                resolveMessage(errorCode)
        );
        problemDetail.setTitle("Validation Failed");
        problemDetail.setType(URI.create(ErrorCodes.VALIDATION_FAILED));
        problemDetail.setProperty("errorCode", errorCode);

        String paramName = ex.getName();
        problemDetail.setProperty("invalid_params", Map.of(paramName, resolveMessage(errorCode)));

        return problemDetail;
    }
}