package com.portfolio.expensetracker.transaction;

import com.portfolio.expensetracker.category.Category;
import com.portfolio.expensetracker.category.CategoryRepository;
import com.portfolio.expensetracker.common.BadRequestException;
import com.portfolio.expensetracker.common.Months;
import com.portfolio.expensetracker.common.PageResponse;
import com.portfolio.expensetracker.common.ResourceNotFoundException;
import com.portfolio.expensetracker.transaction.dto.TransactionRequest;
import com.portfolio.expensetracker.transaction.dto.TransactionResponse;
import com.portfolio.expensetracker.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

import static com.portfolio.expensetracker.transaction.TransactionSpecifications.*;

/**
 * Business logic for income/expense transactions.
 *
 * <p><b>Request flow:</b> {@code TransactionController} (HTTP + validation) → this service
 * (ownership + business rules) → {@code TransactionRepository} / {@code CategoryRepository} (SQL).
 *
 * <p><b>Rules enforced here</b>
 * <ul>
 *   <li>Every read/write is scoped to the calling user ({@code findByIdAndUserId} → 404 otherwise).</li>
 *   <li>The category must exist AND belong to the user (→ 404 otherwise; you can't file a transaction
 *       under someone else's category).</li>
 *   <li>The category's type must equal the transaction's type (→ 400), e.g. no "Salary" expense.</li>
 * </ul>
 */
@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactions;
    private final CategoryRepository categories;

    public TransactionService(TransactionRepository transactions, CategoryRepository categories) {
        this.transactions = transactions;
        this.categories = categories;
    }

    /**
     * Filtered, sorted, paginated list. Every filter is optional.
     *
     * @param month      "YYYY-MM" or null for all time (400 if malformed)
     * @param type       INCOME / EXPENSE or null
     * @param categoryId category id or null
     * @param query      text to search in descriptions, or null
     * @param pageable   page number, size and sort, built by Spring from ?page=&size=&sort=
     */
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(User user, String month, TransactionType type, Long categoryId,
                                                    String query, Pageable pageable) {
        YearMonth ym = (month == null || month.isBlank()) ? null : Months.parse(month);
        var spec = ownedBy(user.getId())
                .and(inMonth(ym))
                .and(hasType(type))
                .and(inCategory(categoryId))
                .and(descriptionContains(query));
        // Page.map converts each entity to a DTO while keeping the paging metadata.
        return PageResponse.from(transactions.findAll(spec, pageable).map(TransactionResponse::from));
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(User user, Long id) {
        return TransactionResponse.from(findOwned(user, id));
    }

    public TransactionResponse create(User user, TransactionRequest request) {
        Transaction transaction = new Transaction(user);
        apply(user, transaction, request);
        return TransactionResponse.from(transactions.save(transaction));
    }

    public TransactionResponse update(User user, Long id, TransactionRequest request) {
        Transaction transaction = findOwned(user, id);
        apply(user, transaction, request);
        // saveAndFlush runs the UPDATE now, so @PreUpdate sets updatedAt before we build the response.
        return TransactionResponse.from(transactions.saveAndFlush(transaction));
    }

    public void delete(User user, Long id) {
        transactions.delete(findOwned(user, id));
    }

    private Transaction findOwned(User user, Long id) {
        return transactions.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction " + id + " not found"));
    }

    /** Copies validated request data onto the entity after checking the category rules. */
    private void apply(User user, Transaction target, TransactionRequest r) {
        Category category = categories.findByIdAndUserId(r.categoryId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category " + r.categoryId() + " not found"));
        if (category.getType() != r.type()) {
            throw new BadRequestException("categoryId", "Category '" + category.getName() + "' is an "
                    + category.getType().name().toLowerCase() + " category and cannot be used for "
                    + r.type().name().toLowerCase() + " transactions");
        }
        target.setType(r.type());
        target.setCategory(category);
        target.setAmount(r.amount());
        target.setDescription(r.description().trim());
        target.setDate(r.date());
        target.setNotes(blankToNull(r.notes()));
    }

    /** Stores "no notes" as NULL rather than "" or "   ", so there's one way to represent "empty". */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
