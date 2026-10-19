package com.portfolio.expensetracker.category.dto;

import com.portfolio.expensetracker.category.Category;
import com.portfolio.expensetracker.transaction.TransactionType;

/**
 * How a category looks in JSON.
 *
 * @param transactionCount how many transactions use it. The UI shows this and explains why such a
 *                         category cannot be deleted (the API would answer 409).
 */
public record CategoryResponse(Long id, String name, String color, TransactionType type, long transactionCount) {

    public static CategoryResponse from(Category c, long transactionCount) {
        return new CategoryResponse(c.getId(), c.getName(), c.getColor(), c.getType(), transactionCount);
    }
}
