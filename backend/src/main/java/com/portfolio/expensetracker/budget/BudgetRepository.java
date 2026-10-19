package com.portfolio.expensetracker.budget;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Database access for {@link Budget}. All lookups are scoped by user id. */
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /**
     * All budgets of a user for one month. {@code @EntityGraph} loads each budget's category in the
     * same query because the response always shows the category name and color.
     */
    @EntityGraph(attributePaths = "category")
    List<Budget> findByUserIdAndMonth(Long userId, String month);

    /** Ownership-scoped lookup (→ 404 for other users' budgets). */
    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    /** Finds the existing budget for the upsert: same user, category and month. */
    Optional<Budget> findByUserIdAndCategoryIdAndMonth(Long userId, Long categoryId, String month);

    /** Used to block changing a category's type while budgets reference it. */
    boolean existsByCategoryId(Long categoryId);

    /**
     * Bulk delete in a single SQL statement. {@code @Modifying} is required for any {@code @Query}
     * that changes data (UPDATE/DELETE) rather than selecting it.
     */
    @Modifying
    @Query("delete from Budget b where b.category.id = :categoryId")
    void deleteByCategoryId(Long categoryId);
}
