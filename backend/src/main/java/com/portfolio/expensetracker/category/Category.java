package com.portfolio.expensetracker.category;

import com.portfolio.expensetracker.transaction.TransactionType;
import com.portfolio.expensetracker.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * A user-defined label for transactions ("Food", "Salary"...), with a display color and a type.
 *
 * <p><b>Where it fits:</b> managed through {@code CategoryController}/{@code CategoryService};
 * referenced by every {@code Transaction} and {@code Budget}. Maps to the {@code categories} table
 * created in {@code V1__create_tables.sql}.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The owner. {@code @ManyToOne}: many categories belong to one user. {@code FetchType.LAZY} means
     * the User row is NOT loaded until {@code getUser()} is actually used (Hibernate inserts a proxy),
     * which avoids an extra query every time a category is read. {@code @JoinColumn} names the FK column.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    /** "#RRGGBB" hex color used for chart slices, swatches and budget bars. */
    @Column(nullable = false, length = 7)
    private String color;

    /** {@code EnumType.STRING} stores "EXPENSE"/"INCOME" rather than 0/1 (safe if the enum is reordered). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected Category() {
    }

    public Category(User user, String name, String color, TransactionType type) {
        this.user = user;
        this.name = name;
        this.color = color;
        this.type = type;
    }

    /** JPA lifecycle callback: runs just before the first INSERT. */
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public Instant getCreatedAt() { return createdAt; }
}
