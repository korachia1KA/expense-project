package com.portfolio.expensetracker.budget;

import com.portfolio.expensetracker.budget.dto.BudgetRequest;
import com.portfolio.expensetracker.budget.dto.BudgetResponse;
import com.portfolio.expensetracker.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for monthly budgets under {@code /api/budgets}.
 * <pre>
 *  GET    /api/budgets?month=2026-10   budgets of that month with progress (default: current month)
 *  PUT    /api/budgets                 create or update (upsert) one budget → 200
 *  DELETE /api/budgets/{id}            → 204
 * </pre>
 * PUT is on the collection (no id in the URL) because the natural key of a budget is
 * (category, month), which travels in the body.
 */
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @GetMapping
    public List<BudgetResponse> list(@AuthenticationPrincipal User user,
                                     @RequestParam(required = false) String month) {
        return service.list(user, month);
    }

    @PutMapping
    public BudgetResponse upsert(@AuthenticationPrincipal User user, @Valid @RequestBody BudgetRequest request) {
        return service.upsert(user, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        service.delete(user, id);
    }
}
