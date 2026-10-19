package com.portfolio.expensetracker.budget;

import com.portfolio.expensetracker.budget.dto.BudgetRequest;
import com.portfolio.expensetracker.budget.dto.BudgetResponse;
import com.portfolio.expensetracker.category.Category;
import com.portfolio.expensetracker.category.CategoryRepository;
import com.portfolio.expensetracker.common.BadRequestException;
import com.portfolio.expensetracker.summary.dto.CategoryTotal;
import com.portfolio.expensetracker.transaction.TransactionRepository;
import com.portfolio.expensetracker.transaction.TransactionType;
import com.portfolio.expensetracker.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static com.portfolio.expensetracker.TestData.category;
import static com.portfolio.expensetracker.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** UNIT tests for budget upsert rules and the spent/remaining/percent math. */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    /** "Today" is fixed at 2026-10-14 so "current month" is always 2026-10 in these tests. */
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_31 = LocalDate.of(2026, 10, 31);

    @Mock
    private BudgetRepository budgets;
    @Mock
    private CategoryRepository categories;
    @Mock
    private TransactionRepository transactions;

    private BudgetService service;
    private User user;
    private Category food;

    @BeforeEach
    void setUp() {
        service = new BudgetService(budgets, categories, transactions, FIXED_CLOCK);
        user = user(1L);
        food = category(3L, user, "Food", TransactionType.EXPENSE);
    }

    @Test
    void upsert_updatesTheExistingBudget_insteadOfCreatingADuplicate() {
        Budget existing = new Budget(user, food, "2026-10");
        existing.setLimitAmount(new BigDecimal("300.00"));
        when(categories.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(food));
        when(budgets.findByUserIdAndCategoryIdAndMonth(1L, 3L, "2026-10")).thenReturn(Optional.of(existing));
        when(budgets.saveAndFlush(existing)).thenReturn(existing);
        when(transactions.totalsByCategory(1L, TransactionType.EXPENSE, OCT_1, OCT_31)).thenReturn(List.of());

        BudgetResponse response = service.upsert(user, new BudgetRequest(3L, "2026-10", new BigDecimal("450.00")));

        assertThat(existing.getLimitAmount()).isEqualByComparingTo("450.00");  // same object updated
        assertThat(response.limit()).isEqualByComparingTo("450.00");
        assertThat(response.spent()).isEqualByComparingTo("0");
        verify(budgets).saveAndFlush(existing);
    }

    @Test
    void upsert_rejectsIncomeCategories() {
        Category salary = category(8L, user, "Salary", TransactionType.INCOME);
        when(categories.findByIdAndUserId(8L, 1L)).thenReturn(Optional.of(salary));

        assertThatThrownBy(() -> service.upsert(user, new BudgetRequest(8L, "2026-10", BigDecimal.TEN)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expense categories");
        verify(budgets, never()).saveAndFlush(any());
    }

    @Test
    void list_defaultsToCurrentMonth_andComputesOverBudgetProgress() {
        Budget budget = new Budget(user, food, "2026-10");
        budget.setLimitAmount(new BigDecimal("80.00"));
        when(budgets.findByUserIdAndMonth(1L, "2026-10")).thenReturn(List.of(budget));
        when(transactions.totalsByCategory(1L, TransactionType.EXPENSE, OCT_1, OCT_31))
                .thenReturn(List.of(new CategoryTotal(3L, "Food", "#123456", new BigDecimal("100.00"))));

        List<BudgetResponse> result = service.list(user, null);   // null month → clock's month

        assertThat(result).hasSize(1);
        BudgetResponse r = result.get(0);
        assertThat(r.spent()).isEqualByComparingTo("100.00");
        assertThat(r.remaining()).isEqualByComparingTo("-20.00");   // negative = over budget
        assertThat(r.percentUsed()).isEqualTo(125.0);
    }
}
