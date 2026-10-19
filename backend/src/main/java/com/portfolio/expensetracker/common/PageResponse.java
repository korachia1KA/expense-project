package com.portfolio.expensetracker.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A stable, frontend-friendly page of results:
 * {@code { "content": [...], "page": 0, "size": 20, "totalElements": 57, "totalPages": 3 }}.
 *
 * <p>Why not return Spring's {@link Page} directly? Its JSON shape is large, includes internal
 * details, and Spring itself warns that it is not guaranteed to stay stable between versions.
 * Owning the shape keeps the API contract under our control.
 *
 * @param <T> the type of item in the page (e.g. TransactionResponse)
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Copies the useful fields out of a Spring Data {@link Page}. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
