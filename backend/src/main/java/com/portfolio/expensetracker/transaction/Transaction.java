package com.portfolio.expensetracker.transaction;

import com.portfolio.expensetracker.category.Category;
import com.portfolio.expensetracker.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One income or expense entry: "2026-10-03, Groceries, -54.20 (Food)".
 *
 * <p><b>Where it fits:</b> created/edited through {@code TransactionController} →
 * {@code TransactionService}; read in aggregate by {@code SummaryService} and {@code BudgetService}.
 * Maps to the {@code transactions} table.
 *
 * <p><b>Why {@link BigDecimal} for money?</b> {@code double} is binary floating point and cannot
 * represent most decimal fractions exactly: {@code 0.1 + 0.2 == 0.30000000000000004}. Over many
 * additions such errors become visible cents. BigDecimal stores exact decimal digits, and the
 * database column is the matching exact type {@code NUMERIC(12,2)}.
 *
 * <p>The amount is always POSITIVE; whether it adds or subtracts is decided by {@link #type}.
 * That keeps sums simple (SUM(amount) WHERE type = 'EXPENSE') and avoids sign mistakes.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** LAZY: loaded on demand. The list query uses an @EntityGraph to fetch it in the same SELECT. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    /** precision = total digits (12), scale = digits after the decimal point (2). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String description;

    /** The day the money moved (not when the row was created). Column renamed because DATE is an SQL keyword. */
    @Column(name = "transaction_date", nullable = false)
    private LocalDate date;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Transaction() {
    }

    public Transaction(User user) {
        this.user = user;
    }

    /** Runs before INSERT: both timestamps start equal. */
    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    /** Runs before every UPDATE Hibernate issues for this entity. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
