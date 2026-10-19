package com.portfolio.expensetracker.common;

/**
 * Thrown when a request is valid but clashes with the current state of the data, e.g.
 * registering an email that already exists, a duplicate category name, or deleting a category
 * that still has transactions. {@link GlobalExceptionHandler} turns it into HTTP 409 Conflict.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
