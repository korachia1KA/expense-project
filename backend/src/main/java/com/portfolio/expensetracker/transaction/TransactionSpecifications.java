package com.portfolio.expensetracker.transaction;

import org.springframework.data.jpa.domain.Specification;

import java.time.YearMonth;

/**
 * Small, composable WHERE-clause building blocks for the transaction list filters.
 *
 * <p>A {@link Specification} is a lambda {@code (root, query, cb) -> Predicate} where
 * {@code root} is the entity being queried, and {@code cb} (CriteriaBuilder) creates conditions.
 * Specifications combine with {@code .and(...)}: the service writes
 * {@code ownedBy(id).and(inMonth(m)).and(hasType(t))...}.
 *
 * <p>Trick: returning {@code null} means "no condition", and {@code and(null)} is ignored. So each
 * optional filter simply returns null when the user didn't supply it - no if/else chains needed.
 * This avoids writing one repository method per combination of filters (2^4 = 16 of them!).
 */
final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    /** ALWAYS applied: restricts results to the caller's own transactions. */
    static Specification<Transaction> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    /** First to last day of the month, inclusive. */
    static Specification<Transaction> inMonth(YearMonth month) {
        return (root, query, cb) -> month == null ? null
                : cb.between(root.get("date"), month.atDay(1), month.atEndOfMonth());
    }

    static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    static Specification<Transaction> inCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    /** Case-insensitive "contains" search on the description: {@code lower(description) like '%text%'}. */
    static Specification<Transaction> descriptionContains(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            // Escape LIKE wildcards so searching for "50%" finds the literal text "50%".
            String escaped = text.trim().toLowerCase()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            return cb.like(cb.lower(root.get("description")), "%" + escaped + "%", '\\');
        };
    }
}
