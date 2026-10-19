package com.portfolio.expensetracker.category;

import com.portfolio.expensetracker.budget.BudgetRepository;
import com.portfolio.expensetracker.category.dto.CategoryRequest;
import com.portfolio.expensetracker.category.dto.CategoryResponse;
import com.portfolio.expensetracker.category.dto.CategoryUsage;
import com.portfolio.expensetracker.common.ConflictException;
import com.portfolio.expensetracker.common.ResourceNotFoundException;
import com.portfolio.expensetracker.transaction.TransactionRepository;
import com.portfolio.expensetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business rules for categories.
 *
 * <p><b>Rules enforced here</b>
 * <ul>
 *   <li>Names are unique per user (case-insensitive) → 409 on duplicates.</li>
 *   <li>A category with transactions cannot be deleted → 409 with an explanation.</li>
 *   <li>The TYPE of a category with transactions or budgets cannot change → 409, because it would
 *       break the "transaction type = category type" rule and "budgets only on expense categories".</li>
 *   <li>Everything is scoped to the calling user → 404 for other users' ids.</li>
 * </ul>
 *
 * <p>{@code @Transactional} on the CLASS applies to every public method: each call runs in one DB
 * transaction that commits when the method returns and rolls back if a RuntimeException escapes.
 */
@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;

    public CategoryService(CategoryRepository categories, TransactionRepository transactions,
                           BudgetRepository budgets) {
        this.categories = categories;
        this.transactions = transactions;
        this.budgets = budgets;
    }

    /**
     * All of the user's categories, each with its transaction count.
     * Counts come from ONE grouped query instead of one count query per category (avoids "N+1 queries").
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> list(User user) {
        Map<Long, Long> usage = transactions.countPerCategory(user.getId()).stream()
                .collect(Collectors.toMap(CategoryUsage::categoryId, CategoryUsage::count));
        return categories.findByUserIdOrderByTypeAscNameAsc(user.getId()).stream()
                .map(c -> CategoryResponse.from(c, usage.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(User user, Long id) {
        Category category = findOwned(user, id);
        return CategoryResponse.from(category, transactions.countByCategoryId(id));
    }

    /** @throws ConflictException if the user already has a category with this name */
    public CategoryResponse create(User user, CategoryRequest request) {
        String name = request.name().trim();
        if (categories.existsByUserIdAndNameIgnoreCase(user.getId(), name)) {
            throw new ConflictException("You already have a category named '" + name + "'");
        }
        Category saved = categories.save(new Category(user, name, request.color().toLowerCase(), request.type()));
        return CategoryResponse.from(saved, 0);
    }

    /**
     * Renames/recolors a category, and changes its type only if nothing depends on it.
     *
     * <p>Note there is no explicit {@code save()} for the changes: the entity was loaded inside this
     * transaction, so Hibernate tracks it ("managed" state) and writes the changes on commit
     * ("dirty checking"). We call {@code saveAndFlush} only to get the UPDATE executed now.
     */
    public CategoryResponse update(User user, Long id, CategoryRequest request) {
        Category category = findOwned(user, id);
        String name = request.name().trim();
        if (categories.existsByUserIdAndNameIgnoreCaseAndIdNot(user.getId(), name, id)) {
            throw new ConflictException("You already have a category named '" + name + "'");
        }
        long used = transactions.countByCategoryId(id);
        if (category.getType() != request.type() && (used > 0 || budgets.existsByCategoryId(id))) {
            throw new ConflictException("Cannot change the type of '" + category.getName()
                    + "' because it already has transactions or budgets");
        }
        category.setName(name);
        category.setColor(request.color().toLowerCase());
        category.setType(request.type());
        return CategoryResponse.from(categories.saveAndFlush(category), used);
    }

    /**
     * Deletes a category that has no transactions. Its budgets go too (ON DELETE CASCADE in SQL).
     *
     * @throws ConflictException (→ 409) when transactions still reference it. Silently deleting
     *         financial history would be surprising and irreversible, so we ask the user to act.
     */
    public void delete(User user, Long id) {
        Category category = findOwned(user, id);
        long used = transactions.countByCategoryId(id);
        if (used > 0) {
            throw new ConflictException("Category '" + category.getName() + "' is used by " + used
                    + (used == 1 ? " transaction" : " transactions")
                    + ". Move or delete those transactions first.");
        }
        budgets.deleteByCategoryId(id);
        categories.delete(category);
    }

    /** Gives a brand-new user the starter categories from {@link DefaultCategories}. */
    public void createDefaults(User user) {
        categories.saveAll(DefaultCategories.ALL.stream()
                .map(d -> new Category(user, d.name(), d.color(), d.type()))
                .toList());
    }

    /** Ownership-scoped lookup shared by the methods above. */
    private Category findOwned(User user, Long id) {
        return categories.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category " + id + " not found"));
    }
}
