package com.portfolio.expensetracker.transaction;

import com.portfolio.expensetracker.category.dto.CategoryUsage;
import com.portfolio.expensetracker.summary.dto.CategoryTotal;
import com.portfolio.expensetracker.summary.dto.DailyTotal;
import com.portfolio.expensetracker.summary.dto.MonthTypeTotal;
import com.portfolio.expensetracker.summary.dto.TypeTotal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Database access for {@link Transaction}.
 *
 * <p>{@link JpaSpecificationExecutor} adds {@code findAll(Specification, Pageable)}: dynamic WHERE
 * clauses assembled from optional filters (see {@link TransactionSpecifications}).
 *
 * <p><b>Aggregate queries.</b> The {@code @Query} methods below are written in JPQL (queries over
 * entities and fields, not tables and columns). They use SUM / COUNT with GROUP BY so that the
 * <i>database</i> adds up the numbers and returns a handful of rows. The alternative, loading every
 * transaction into Java and summing in a loop, transfers far more data and gets slower as history
 * grows. Databases are built for exactly this, and indexes on (user_id, transaction_date) help.
 *
 * <p>{@code select new com...TypeTotal(...)} is a "constructor expression": each result row is
 * passed straight into that record's constructor, giving typed results instead of {@code Object[]}.
 *
 * <p>Named parameters ({@code :userId}) are bound from the method parameter names (Spring Boot compiles
 * with {@code -parameters}, so the names are available at runtime).
 */
public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    /** Ownership-scoped lookup: another user's id → empty → 404. */
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    /**
     * Overrides the inherited method only to add {@code @EntityGraph}: fetch each transaction's category
     * in the SAME query (a JOIN) instead of one extra query per row when the DTO reads the category
     * name. Without it, a page of 20 rows would cost 21 queries: the classic "N+1 problem".
     */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    /** Used to block deleting a category that is still in use. */
    long countByCategoryId(Long categoryId);

    /** Number of transactions per category for one user, in one query (for the categories page). */
    @Query("""
            select new com.portfolio.expensetracker.category.dto.CategoryUsage(t.category.id, count(t))
            from Transaction t
            where t.user.id = :userId
            group by t.category.id
            """)
    List<CategoryUsage> countPerCategory(Long userId);

    /** Total income and total expense between two dates (inclusive). */
    @Query("""
            select new com.portfolio.expensetracker.summary.dto.TypeTotal(t.type, sum(t.amount))
            from Transaction t
            where t.user.id = :userId and t.date between :from and :to
            group by t.type
            """)
    List<TypeTotal> sumByType(Long userId, LocalDate from, LocalDate to);

    /** Expense total per category between two dates, biggest first. */
    @Query("""
            select new com.portfolio.expensetracker.summary.dto.CategoryTotal(c.id, c.name, c.color, sum(t.amount))
            from Transaction t join t.category c
            where t.user.id = :userId and t.type = :type and t.date between :from and :to
            group by c.id, c.name, c.color
            order by sum(t.amount) desc
            """)
    List<CategoryTotal> totalsByCategory(Long userId, TransactionType type, LocalDate from, LocalDate to);

    /** Expense total for each day that had spending (days without spending are absent). */
    @Query("""
            select new com.portfolio.expensetracker.summary.dto.DailyTotal(t.date, sum(t.amount))
            from Transaction t
            where t.user.id = :userId and t.type = :type and t.date between :from and :to
            group by t.date
            order by t.date
            """)
    List<DailyTotal> dailyTotals(Long userId, TransactionType type, LocalDate from, LocalDate to);

    /**
     * Totals per calendar month and type. {@code year()} and {@code month()} are standard JPQL/HQL
     * functions that Hibernate translates to the right SQL for H2 and PostgreSQL ({@code extract(...)}).
     */
    @Query("""
            select new com.portfolio.expensetracker.summary.dto.MonthTypeTotal(
                year(t.date), month(t.date), t.type, sum(t.amount))
            from Transaction t
            where t.user.id = :userId and t.date between :from and :to
            group by year(t.date), month(t.date), t.type
            """)
    List<MonthTypeTotal> monthlyTotals(Long userId, LocalDate from, LocalDate to);
}
