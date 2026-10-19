package com.portfolio.expensetracker.transaction;

import com.portfolio.expensetracker.category.Category;
import com.portfolio.expensetracker.category.CategoryRepository;
import com.portfolio.expensetracker.common.BadRequestException;
import com.portfolio.expensetracker.common.ResourceNotFoundException;
import com.portfolio.expensetracker.transaction.dto.TransactionRequest;
import com.portfolio.expensetracker.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static com.portfolio.expensetracker.TestData.category;
import static com.portfolio.expensetracker.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UNIT test for {@link TransactionService}: no Spring, no database, runs in milliseconds.
 *
 * <p>{@code @ExtendWith(MockitoExtension.class)} lets Mockito create the {@code @Mock} fields: fake
 * repositories whose answers we script with {@code when(...).thenReturn(...)} and whose calls we
 * check with {@code verify(...)}. This isolates the business rules from the database.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactions;
    @Mock
    private CategoryRepository categories;

    private TransactionService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new TransactionService(transactions, categories);
        user = user(1L);
    }

    private static TransactionRequest request(TransactionType type, long categoryId) {
        return new TransactionRequest(type, new BigDecimal("12.50"), "  Coffee beans  ",
                LocalDate.of(2026, 10, 1), categoryId, "   ");
    }

    @Test
    void create_rejectsCategoryWhoseTypeDoesNotMatch() {
        // "Salary" is an INCOME category; using it for an EXPENSE must fail with 400.
        Category salary = category(8L, user, "Salary", TransactionType.INCOME);
        when(categories.findByIdAndUserId(8L, 1L)).thenReturn(Optional.of(salary));

        assertThatThrownBy(() -> service.create(user, request(TransactionType.EXPENSE, 8L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("income category")
                .extracting("field").isEqualTo("categoryId");
        verify(transactions, never()).save(any());
    }

    @Test
    void create_throwsNotFound_whenCategoryBelongsToAnotherUser() {
        // The query is scoped by user id, so someone else's category is simply "not found".
        when(categories.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(user, request(TransactionType.EXPENSE, 99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category 99");
    }

    @Test
    void create_trimsDescription_andStoresBlankNotesAsNull() {
        Category food = category(1L, user, "Food", TransactionType.EXPENSE);
        when(categories.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(food));
        // Return whatever entity is passed to save(), like a real repository would.
        when(transactions.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(user, request(TransactionType.EXPENSE, 1L));

        // ArgumentCaptor grabs the exact object the service passed to save() so we can inspect it.
        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactions).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(response.description()).isEqualTo("Coffee beans");
        assertThat(response.notes()).isNull();
        assertThat(response.amount()).isEqualByComparingTo("12.50");
        assertThat(response.category().name()).isEqualTo("Food");
    }

    @Test
    void update_throwsNotFound_forAnotherUsersTransaction() {
        when(transactions.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(user, 5L, request(TransactionType.EXPENSE, 1L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transactions, never()).saveAndFlush(any());
    }
}
