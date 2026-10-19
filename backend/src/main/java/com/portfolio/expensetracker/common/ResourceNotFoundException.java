package com.portfolio.expensetracker.common;

/**
 * Thrown by services when a record does not exist <b>or belongs to another user</b>.
 * {@link GlobalExceptionHandler} turns it into HTTP 404.
 *
 * <p>Answering 404 (not 403 Forbidden) for someone else's data is intentional: a 403 would confirm
 * that the id exists, leaking information. With 404 an attacker cannot tell "not yours" from "not there".
 *
 * <p>It extends {@link RuntimeException} (an unchecked exception) so it does not need to be declared
 * in every method signature, and so Spring's {@code @Transactional} rolls back automatically.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
