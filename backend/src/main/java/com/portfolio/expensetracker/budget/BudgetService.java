package com.portfolio.expensetracker.budget;

import com.portfolio.expensetracker.budget.dto.BudgetRequest;
import com.portfolio.expensetracker.budget.dto.BudgetResponse;
import com.portfolio.expensetracker.category.Category;
import com.portfolio.expensetracker.category.CategoryRepository;
import com.portfolio.expensetracker.common.BadRequestException;
import com.portfolio.expensetracker.common.Money;
import com.portfolio.expensetracker.common.Months;
import com.portfolio.expensetracker.common.ResourceNotFoundException;
import com.portfolio.expensetracker.summary.dto.CategoryTotal;
import com.portfolio.expensetracker.transaction.TransactionRepository;
import com.portfolio.expensetracker.transaction.TransactionType;
import com.portfolio.expensetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Monthly budgets: list with progress, upsert, delete.
 *
 * <p><b>Rules enforced here</b>
 * <ul>
 *   <li>Budgets only make sense for EXPENSE categories (→ 400 for income categories).</li>
 *   <li>One budget per (user, category, month): PUT creates it or updates the existing one.</li>
 *   <li>Everything scoped to the calling user (→ 404 for other users' categories/budgets).</li>
 * </ul>
 */
@Service
@Transactional
public class BudgetService {

    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final Clock clock;

    public BudgetService(BudgetRepository budgets, CategoryRepository categories,
                         TransactionRepository transactions, Clock clock) {
        this.budgets = budgets;
        this.categories = categories;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * All budgets for the month (default: current month), each with spent/remaining/percentUsed.
     * Spending comes from ONE grouped SQL query for the whole month, not one query per budget.
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> list(User user, String month) {
        YearMonth ym = Months.parseOrCurrent(month, clock);
        Map<Long, BigDecimal> spent = spentPerCategory(user, ym);
        return budgets.findByUserIdAndMonth(user.getId(), ym.toString()).stream()
                .map(b -> toResponse(b, spent.get(b.getCategory().getId())))
                .sorted(Comparator.comparing(BudgetResponse::categoryName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Creates or updates ("upserts") the budget for a category and month.
     *
     * <p><b>Why upsert?</b> From the user's point of view there is one action: "set the Food budget for
     * October to 400". They shouldn't need to know whether a budget already exists, and the client
     * shouldn't need to choose between POST and PUT. PUT fits because the operation is idempotent:
     * sending the same request twice leaves the same result.
     */
    public BudgetResponse upsert(User user, BudgetRequest request) {
        Category category = categories.findByIdAndUserId(request.categoryId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category " + request.categoryId() + " not found"));
        if (category.getType() != TransactionType.EXPENSE) {
            throw new BadRequestException("categoryId", "Budgets can only be set on expense categories");
        }
        YearMonth ym = Months.parse(request.month());

        Budget budget = budgets.findByUserIdAndCategoryIdAndMonth(user.getId(), category.getId(), ym.toString())
                .orElseGet(() -> new Budget(user, category, ym.toString()));
        budget.setLimitAmount(request.limit());
        Budget saved = budgets.saveAndFlush(budget);

        return toResponse(saved, spentPerCategory(user, ym).get(category.getId()));
    }

    public void delete(User user, Long id) {
        Budget budget = budgets.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget " + id + " not found"));
        budgets.delete(budget);
    }

    /** categoryId → total expense in that month, from the aggregate query. */
    private Map<Long, BigDecimal> spentPerCategory(User user, YearMonth ym) {
        return transactions.totalsByCategory(user.getId(), TransactionType.EXPENSE, ym.atDay(1), ym.atEndOfMonth())
                .stream()
                .collect(Collectors.toMap(CategoryTotal::categoryId, CategoryTotal::total));
    }

    private static BudgetResponse toResponse(Budget b, BigDecimal spentOrNull) {
        BigDecimal spent = Money.orZero(spentOrNull);
        BigDecimal limit = b.getLimitAmount();
        return new BudgetResponse(b.getId(), b.getCategory().getId(), b.getCategory().getName(),
                b.getCategory().getColor(), b.getMonth(), limit, spent, limit.subtract(spent),
                Money.percent(spent, limit));
    }
}
