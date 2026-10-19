package com.portfolio.expensetracker.common;

/**
 * Thrown for business-rule violations that simple field annotations cannot express, because they
 * involve more than one value or need the database. Examples:
 * <ul>
 *   <li>an EXPENSE transaction assigned to an INCOME category;</li>
 *   <li>a budget on an INCOME category;</li>
 *   <li>a month parameter that is not in YYYY-MM format.</li>
 * </ul>
 * {@link GlobalExceptionHandler} turns it into HTTP 400 Bad Request.
 *
 * <p>Optionally carries the name of the offending field so the response can include it in
 * {@code fieldErrors}, exactly like annotation-based validation errors.
 */
public class BadRequestException extends RuntimeException {

    private final String field;

    public BadRequestException(String message) {
        this(null, message);
    }

    public BadRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** The request field this error is about, or {@code null} if it is not about one field. */
    public String getField() {
        return field;
    }
}
