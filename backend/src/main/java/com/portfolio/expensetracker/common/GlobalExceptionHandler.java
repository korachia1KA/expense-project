package com.portfolio.expensetracker.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts exceptions thrown anywhere in a controller or service into clean JSON {@link ApiError}
 * responses with the right HTTP status code.
 *
 * <p>{@code @RestControllerAdvice} = "advice" that applies to ALL {@code @RestController}s. When a
 * controller method throws, Spring looks for an {@code @ExceptionHandler} here whose parameter type
 * matches the exception and uses its return value as the response.
 *
 * <p>The payoff: services just {@code throw new ResourceNotFoundException(...)} and never deal with
 * HTTP; controllers contain no try/catch; and every error has the same JSON shape.
 *
 * <p>Mapping table:
 * <pre>
 *  MethodArgumentNotValidException (@Valid failed)  → 400 + fieldErrors
 *  BadRequestException (business rule)               → 400 (+ fieldErrors when a field is known)
 *  malformed JSON / wrong param type / bad sort      → 400 "Malformed request"
 *  BadCredentialsException (wrong password)          → 401
 *  ResourceNotFoundException (missing or not yours)  → 404
 *  ConflictException (duplicate, in use)             → 409
 *  DataIntegrityViolationException (DB constraint)   → 409
 * </pre>
 * Anything else falls through to Spring Boot's default 500 handler (details are not leaked).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** {@code @Valid} on a {@code @RequestBody} failed: report every invalid field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        // LinkedHashMap keeps insertion order; putIfAbsent keeps only the first message per field.
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fieldErrors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ApiError body = new ApiError(Instant.now(), 400, "Bad Request", "Validation failed", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /** Business-rule violation detected by a service (e.g. category type mismatch). */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
        Map<String, String> fieldErrors = ex.getField() == null ? Map.of() : Map.of(ex.getField(), ex.getMessage());
        ApiError body = new ApiError(Instant.now(), 400, "Bad Request", ex.getMessage(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Unreadable JSON, a path/query parameter of the wrong type ("/api/transactions/abc"), an unknown
     * enum value, or {@code ?sort=doesNotExist} (Spring Data throws PropertyReferenceException).
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            PropertyReferenceException.class})
    public ResponseEntity<ApiError> handleBadInput(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Safety net: a database constraint (UNIQUE, FOREIGN KEY, CHECK) rejected a write. Services check
     * these rules first and throw friendlier exceptions, but two requests racing each other (e.g. two
     * simultaneous budget upserts) can slip past the check; the database then has the final say.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "The change conflicts with existing data. Please reload and try again.");
    }

    /** Thrown by AuthService on a wrong email/password. The message never says WHICH one was wrong. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), status.getReasonPhrase(), message));
    }
}
