package com.portfolio.expensetracker.summary;

import com.portfolio.expensetracker.budget.Budget;
import com.portfolio.expensetracker.budget.BudgetRepository;
import com.portfolio.expensetracker.category.Category;
import com.portfolio.expensetracker.common.BadRequestException;
import com.portfolio.expensetracker.summary.dto.*;
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

import static com.portfolio.expensetracker.TestData.category;
import static com.portfolio.expensetracker.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * UNIT tests for the dashboard math: totals, savings rate, budget merge and gap filling.
 * The repository's aggregate queries are mocked, so we control exactly what "the database" returns.
 */
@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_31 = LocalDate.of(2026, 10, 31);

    @Mock
    private TransactionRepository transactions;
    @Mock
    private BudgetRepository budgets;

    private SummaryService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new SummaryService(transactions, budgets, FIXED_CLOCK);
        user = user(1L);
        // Default: every query returns nothing. "lenient" = don't complain if a test never triggers a stub.
        lenient().when(transactions.sumByType(any(), any(), any())).thenReturn(List.of());
        lenient().when(transactions.totalsByCategory(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(transactions.dailyTotals(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(transactions.monthlyTotals(any(), any(), any())).thenReturn(List.of());
        lenient().when(budgets.findByUserIdAndMonth(any(), any())).thenReturn(List.of());
    }

    @Test
    void computesTotalsBalanceAndSavingsRate() {
        when(transactions.sumByType(1L, OCT_1, OCT_31)).thenReturn(List.of(
                new TypeTotal(TransactionType.INCOME, new BigDecimal("3000.00")),
                new TypeTotal(TransactionType.EXPENSE, new BigDecimal("2250.50"))));

        SummaryResponse s = service.summary(user, "2026-10");

        assertThat(s.totalIncome()).isEqualByComparingTo("3000.00");
        assertThat(s.totalExpense()).isEqualByComparingTo("2250.50");
        assertThat(s.balance()).isEqualByComparingTo("749.50");
        assertThat(s.savingsRate()).isEqualTo(25.0);   // 749.50 / 3000 = 24.98% → rounded to 25.0
    }

    @Test
    void withNoIncome_savingsRateIsZero_notADivisionByZero() {
        when(transactions.sumByType(1L, OCT_1, OCT_31)).thenReturn(List.of(
                new TypeTotal(TransactionType.EXPENSE, new BigDecimal("10.00"))));

        SummaryResponse s = service.summary(user, "2026-10");

        assertThat(s.balance()).isEqualByComparingTo("-10.00");
        assertThat(s.savingsRate()).isZero();
    }

    @Test
    void dailySpending_hasEveryDayOfTheMonth_withZerosForEmptyDays() {
        // February 2028 is a leap year: the series must have 29 entries.
        LocalDate feb1 = LocalDate.of(2028, 2, 1);
        LocalDate feb29 = LocalDate.of(2028, 2, 29);
        when(transactions.dailyTotals(1L, TransactionType.EXPENSE, feb1, feb29)).thenReturn(List.of(
                new DailyTotal(LocalDate.of(2028, 2, 10), new BigDecimal("42.00"))));

        SummaryResponse s = service.summary(user, "2028-02");

        assertThat(s.dailySpending()).hasSize(29);
        assertThat(s.dailySpending().get(0).date()).isEqualTo(feb1);
        assertThat(s.dailySpending().get(0).amount()).isEqualByComparingTo("0");
        assertThat(s.dailySpending().get(9).amount()).isEqualByComparingTo("42.00");
        assertThat(s.dailySpending().get(28).date()).isEqualTo(feb29);
    }

    @Test
    void trend_coversSixMonthsOldestFirst_includingEmptyMonths_andDefaultsToClockMonth() {
        // Window for October 2026: May..October 2026.
        when(transactions.monthlyTotals(eq(1L), eq(LocalDate.of(2026, 5, 1)), eq(OCT_31))).thenReturn(List.of(
                new MonthTypeTotal(2026, 7, TransactionType.INCOME, new BigDecimal("3200.00")),
                new MonthTypeTotal(2026, 7, TransactionType.EXPENSE, new BigDecimal("1800.00")),
                new MonthTypeTotal(2026, 10, TransactionType.EXPENSE, new BigDecimal("90.00"))));

        SummaryResponse s = service.summary(user, null);   // null → current month from the fixed clock

        assertThat(s.month()).isEqualTo("2026-10");
        assertThat(s.trend()).extracting(MonthlyTrend::month)
                .containsExactly("2026-05", "2026-06", "2026-07", "2026-08", "2026-09", "2026-10");
        assertThat(s.trend().get(0).income()).isEqualByComparingTo("0");       // empty month filled
        assertThat(s.trend().get(2).income()).isEqualByComparingTo("3200.00");
        assertThat(s.trend().get(2).expense()).isEqualByComparingTo("1800.00");
        assertThat(s.trend().get(5).expense()).isEqualByComparingTo("90.00");
    }

    @Test
    void spendingByCategory_mergesBudgets_includingBudgetsWithoutSpending() {
        Category food = category(1L, user, "Food", TransactionType.EXPENSE);
        Category fun = category(2L, user, "Entertainment", TransactionType.EXPENSE);
        Budget foodBudget = new Budget(user, food, "2026-10");
        foodBudget.setLimitAmount(new BigDecimal("400.00"));
        Budget funBudget = new Budget(user, fun, "2026-10");
        funBudget.setLimitAmount(new BigDecimal("80.00"));
        when(budgets.findByUserIdAndMonth(1L, "2026-10")).thenReturn(List.of(foodBudget, funBudget));
        when(transactions.totalsByCategory(1L, TransactionType.EXPENSE, OCT_1, OCT_31)).thenReturn(List.of(
                new CategoryTotal(1L, "Food", "#f97316", new BigDecimal("300.00")),
                new CategoryTotal(5L, "Transport", "#0ea5e9", new BigDecimal("50.00"))));

        List<CategorySpending> rows = service.summary(user, "2026-10").spendingByCategory();

        assertThat(rows).extracting(CategorySpending::name).containsExactly("Food", "Transport", "Entertainment");
        assertThat(rows.get(0).budget()).isEqualByComparingTo("400.00");
        assertThat(rows.get(0).percentUsed()).isEqualTo(75.0);
        assertThat(rows.get(1).budget()).isNull();          // no budget → null, not 0
        assertThat(rows.get(1).percentUsed()).isNull();
        assertThat(rows.get(2).spent()).isEqualByComparingTo("0");
        assertThat(rows.get(2).percentUsed()).isEqualTo(0.0);
    }

    @Test
    void rejectsMalformedMonth() {
        assertThatThrownBy(() -> service.summary(user, "2026-13"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("YYYY-MM");
    }
}
