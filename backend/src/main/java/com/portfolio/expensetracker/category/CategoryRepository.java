package com.portfolio.expensetracker.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Database access for {@link Category}. Every method takes a {@code userId} so a query can never
 * accidentally return another user's categories.
 *
 * <p>All methods are derived queries: Spring Data builds the SQL from the method name.
 * For example {@code findByUserIdOrderByTypeAscNameAsc} →
 * {@code where user_id = ? order by type asc, name asc}. {@code UserId} navigates {@code user.id}.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** All of a user's categories, EXPENSE before INCOME (alphabetical enum order), then by name. */
    List<Category> findByUserIdOrderByTypeAscNameAsc(Long userId);

    /** Ownership-scoped lookup: returns empty for ids that belong to someone else (→ 404). */
    Optional<Category> findByIdAndUserId(Long id, Long userId);

    /** Duplicate-name check when creating. */
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    /** Duplicate-name check when renaming: ignore the category being edited itself. */
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
}
