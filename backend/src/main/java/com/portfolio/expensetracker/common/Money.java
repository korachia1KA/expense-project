package com.portfolio.expensetracker.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Small arithmetic helpers for money values, shared by the budget and summary features.
 *
 * <p>All money math uses {@link BigDecimal} (exact decimals). Percentages are returned as
 * {@code double} because they are display values (e.g. 87.5) and are never added up again.
 */
public final class Money {

    private Money() {
    }

    /** Null-safe: treats a missing total (no rows in a SUM query) as zero. */
    public static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * {@code part / whole * 100}, rounded to one decimal place (87.46 → 87.5). Returns 0 when
     * {@code whole} is zero, because "percent of nothing" is undefined and we don't want a crash.
     *
     * <p>BigDecimal division needs an explicit scale and {@link RoundingMode}: 1/3 has infinitely many
     * decimals, and without them {@code divide} would throw ArithmeticException.
     * HALF_UP is the "school" rounding everyone expects (x.x5 rounds up).
     */
    public static double percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) {
            return 0.0;
        }
        return orZero(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(whole, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
