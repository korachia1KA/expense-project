package com.portfolio.expensetracker.category.dto;

/**
 * One row of the "how many transactions per category" aggregate query
 * ({@code TransactionRepository.countPerCategory}). JPQL creates it with a constructor expression:
 * {@code select new ...CategoryUsage(t.category.id, count(t)) ... group by t.category.id}.
 */
public record CategoryUsage(Long categoryId, long count) {
}
