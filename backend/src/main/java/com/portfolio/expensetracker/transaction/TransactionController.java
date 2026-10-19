package com.portfolio.expensetracker.transaction;

import com.portfolio.expensetracker.common.PageResponse;
import com.portfolio.expensetracker.transaction.dto.TransactionRequest;
import com.portfolio.expensetracker.transaction.dto.TransactionResponse;
import com.portfolio.expensetracker.user.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for transactions under {@code /api/transactions}.
 * <pre>
 *  GET    /api/transactions?month=2026-10&type=EXPENSE&categoryId=3&q=coffee&page=0&size=20&sort=amount,desc
 *  GET    /api/transactions/{id}
 *  POST   /api/transactions         → 201
 *  PUT    /api/transactions/{id}
 *  DELETE /api/transactions/{id}    → 204
 * </pre>
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    /**
     * {@code @RequestParam(required = false)} makes each filter optional (null when absent).
     * The type string is converted to the enum automatically; an unknown value → 400.
     *
     * <p>Spring builds the {@link Pageable} from {@code ?page=&size=&sort=}. {@code @PageableDefault}
     * sets the defaults (20 per page) and {@code @SortDefault}s sort newest first, then by id so
     * transactions on the same day keep a stable order between pages.
     */
    @GetMapping
    public PageResponse<TransactionResponse> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(name = "q", required = false) String query,
            @PageableDefault(size = 20)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "date", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "id", direction = Sort.Direction.DESC)}) Pageable pageable) {
        return service.search(user, month, type, categoryId, query, pageable);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.get(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@AuthenticationPrincipal User user,
                                      @Valid @RequestBody TransactionRequest request) {
        return service.create(user, request);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@AuthenticationPrincipal User user, @PathVariable Long id,
                                      @Valid @RequestBody TransactionRequest request) {
        return service.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        service.delete(user, id);
    }
}
