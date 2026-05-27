package ru.copperside.payadmin.common.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import ru.copperside.payadmin.common.application.UpstreamProblemException;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_BASE = "https://contracts.newpay/errors/";

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemEnvelope> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fields.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "validation-error",
                "Validation failed",
                ex.getMessage(),
                Map.of("fields", fields)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemEnvelope> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));
        return problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "validation-error",
                "Validation failed",
                ex.getMessage(),
                Map.of("fields", fields)
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemEnvelope> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "validation-error",
                "Validation failed",
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemEnvelope> handleBadRequest(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "bad-request", "Bad request", ex.getMessage(), null);
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    public ResponseEntity<ProblemEnvelope> handleUpstreamUnavailable(UpstreamUnavailableException ex) {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "UPSTREAM_UNAVAILABLE",
                "upstream-unavailable",
                "Upstream unavailable",
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(UpstreamProblemException.class)
    public ResponseEntity<ProblemEnvelope> handleUpstreamProblem(UpstreamProblemException ex) {
        return ResponseEntity.status(ex.statusCode())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ex.problem());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemEnvelope> handleNoResource(NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "not-found", "Not Found", "Resource not found: " + ex.getResourcePath(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemEnvelope> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", "internal", "Internal server error", null, null);
    }

    private ResponseEntity<ProblemEnvelope> problem(
            HttpStatus status,
            String code,
            String typeSuffix,
            String title,
            String message,
            Object details
    ) {
        ProblemDetail detail = new ProblemDetail(
                TYPE_BASE + typeSuffix,
                title,
                status.value(),
                code,
                message,
                details,
                traceId()
        );
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemEnvelope.of(detail, clock));
    }

    private String traceId() {
        String traceId = MDC.get(RequestIdFilter.MDC_KEY);
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }
}

