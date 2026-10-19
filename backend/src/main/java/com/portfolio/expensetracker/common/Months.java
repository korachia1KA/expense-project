package com.portfolio.expensetracker.common;

import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Helpers for the "YYYY-MM" month strings used across the API ({@code ?month=2026-10}).
 *
 * <p>Months travel as plain strings in URLs and JSON because that format is human-readable, sorts
 * correctly as text, and is exactly what an HTML {@code <input type="month">} produces. Inside the
 * code we convert them to {@link YearMonth}, which knows how many days a month has, leap years, etc.
 *
 * <p>A {@code final} class with a private constructor is the Java idiom for "static helpers only".
 */
public final class Months {

    /** Regex used by bean validation on request bodies: 4-digit year, dash, month 01-12. */
    public static final String PATTERN = "^\\d{4}-(0[1-9]|1[0-2])$";

    private Months() {
    }

    /**
     * Parses "YYYY-MM". If {@code value} is null or blank, returns the current month according to
     * {@code clock} (so {@code GET /api/summary} with no parameter shows "this month").
     *
     * @throws BadRequestException (→ HTTP 400) if the text is not a valid month
     */
    public static YearMonth parseOrCurrent(String value, Clock clock) {
        if (value == null || value.isBlank()) {
            return YearMonth.now(clock);
        }
        return parse(value);
    }

    /**
     * Parses a required "YYYY-MM" string.
     *
     * @throws BadRequestException (→ HTTP 400) if the text is not a valid month
     */
    public static YearMonth parse(String value) {
        if (value == null || !value.trim().matches(PATTERN)) {
            throw new BadRequestException("month", "must be in YYYY-MM format");
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("month", "must be in YYYY-MM format");
        }
    }
}
