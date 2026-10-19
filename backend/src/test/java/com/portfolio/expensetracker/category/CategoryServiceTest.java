package com.portfolio.expensetracker.category;

import com.portfolio.expensetracker.budget.BudgetRepository;
import com.portfolio.expensetracker.category.dto.CategoryRequest;
import com.portfolio.expensetracker.common.ConflictException;
import com.portfolio.expensetracker.transaction.TransactionRepository;
import com.portfolio.expensetracker.transaction.TransactionType;
import com.portfolio.expensetracker.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.portfolio.expensetracker.TestData.category;
import static com.portfolio.expensetracker.TestData.user;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** UNIT tests for the category rules: unique names, delete-in-use and type changes. */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categories;
    @Mock
    private TransactionRepository transactions;
    @Mock
    private BudgetRepository budgets;

    private CategoryService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categories, transactions, budgets);
        user = user(1L);
    }

    @Test
    void delete_categoryWithTransactions_throwsConflict_andDeletesNothing() {
        Category food = category(3L, user, "Food", TransactionType.EXPENSE);
        when(categories.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(food));
        when(transactions.countByCategoryId(3L)).thenReturn(12L);

        assertThatThrownBy(() -> service.delete(user, 3L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("used by 12 transactions");
        verify(categories, never()).delete(any());
        verify(budgets, never()).deleteByCategoryId(any());
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(categories.existsByUserIdAndNameIgnoreCase(1L, "Food")).thenReturn(true);

        assertThatThrownBy(() -> service.create(user, new CategoryRequest(" Food ", "#ff0000", TransactionType.EXPENSE)))
                .isInstanceOf(ConflictException.class);
        verify(categories, never()).save(any());
    }

    @Test
    void update_changingTypeOfCategoryInUse_throwsConflict() {
        Category food = category(3L, user, "Food", TransactionType.EXPENSE);
        when(categories.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(food));
        when(transactions.countByCategoryId(3L)).thenReturn(2L);

        assertThatThrownBy(() -> service.update(user, 3L, new CategoryRequest("Food", "#ff0000", TransactionType.INCOME)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot change the type");
    }
}
