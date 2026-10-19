package com.portfolio.expensetracker.budget;

import com.portfolio.expensetracker.category.Category;
import com.portfolio.expensetracker.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A monthly spending limit for one EXPENSE category, e.g. "Food, 2026-10, 400.00".
 *
 * <p><b>Where it fits:</b> written through {@code BudgetController} → {@code BudgetService};
 * read by the budgets page and by {@code SummaryService} (to show "spent vs. budget").
 * The table has a UNIQUE (user_id, category_id, budget_month) constraint: one budget per
 * category per month, which is what makes PUT /api/budgets an "upsert".
 *
 * <p>How much was actually spent is NOT stored here. It is always computed from transactions, so it
 * can never get out of sync when a transaction is edited or deleted ("single source of truth").
 */
@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** "YYYY-MM". A String keeps the mapping trivial and the column human-readable. */
    @Column(name = "budget_month", nullable = false, length = 7)
    private String month;

    /** The spending limit. Called "limit" in JSON; LIMIT is an SQL keyword, hence the column name. */
    @Column(name = "limit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Budget() {
    }

    public Budget(User user, Category category, String month) {
        this.user = user;
        this.category = category;
        this.month = month;
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Category getCategory() { return category; }
    public String getMonth() { return month; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
