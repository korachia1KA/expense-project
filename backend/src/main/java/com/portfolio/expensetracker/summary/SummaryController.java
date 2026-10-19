package com.portfolio.expensetracker.summary;

import com.portfolio.expensetracker.summary.dto.SummaryResponse;
import com.portfolio.expensetracker.user.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/summary?month=2026-10} → dashboard data for that month (default: current month).
 * Read-only; all the logic lives in {@link SummaryService}.
 */
@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final SummaryService service;

    public SummaryController(SummaryService service) {
        this.service = service;
    }

    @GetMapping
    public SummaryResponse summary(@AuthenticationPrincipal User user,
                                   @RequestParam(required = false) String month) {
        return service.summary(user, month);
    }
}
