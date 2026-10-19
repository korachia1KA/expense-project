package com.portfolio.expensetracker.common;

import java.time.Instant;
import java.util.Map;

/**
 * The JSON body of EVERY error response, for example:
 * <pre>{@code
 * { "timestamp": "2026-10-14T10:00:00Z", "status": 400, "error": "Bad Request",
 *   "message": "Validation failed", "fieldErrors": { "amount": "must be greater than 0" } }
 * }</pre>
 *
 * <p>A single, predictable shape means the Angular app needs only one helper
 * ({@code shared/error-message.ts}) to show any error to the user.
 *
 * <p>This is a Java <b>record</b>: an immutable data carrier. The compiler generates the constructor,
 * accessors ({@code status()}, {@code message()}...), equals/hashCode and toString. Jackson serialises
 * records to JSON using the component names as keys.
 *
 * @param fieldErrors per-field validation messages (empty for non-validation errors)
 */
public record ApiError(Instant timestamp, int status, String error, String message, Map<String, String> fieldErrors) {

    /** Convenience factory for errors that are not about a specific field. */
    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, Map.of());
    }
}
