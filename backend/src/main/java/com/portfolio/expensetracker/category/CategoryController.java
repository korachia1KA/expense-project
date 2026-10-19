package com.portfolio.expensetracker.category;

import com.portfolio.expensetracker.category.dto.CategoryRequest;
import com.portfolio.expensetracker.category.dto.CategoryResponse;
import com.portfolio.expensetracker.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for categories under {@code /api/categories}. Standard CRUD:
 * <pre>
 *  GET    /api/categories        list (not paginated: a user has a handful of categories)
 *  GET    /api/categories/{id}   one
 *  POST   /api/categories        create  → 201
 *  PUT    /api/categories/{id}   replace → 200
 *  DELETE /api/categories/{id}   delete  → 204, or 409 if transactions use it
 * </pre>
 * Every handler receives the logged-in user via {@code @AuthenticationPrincipal} and passes it to
 * the service, which scopes all queries to that user.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal User user) {
        return service.list(user);
    }

    /** {@code @PathVariable} binds the "{id}" part of the URL; a non-number gives 400. */
    @GetMapping("/{id}")
    public CategoryResponse get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.get(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@AuthenticationPrincipal User user, @Valid @RequestBody CategoryRequest request) {
        return service.create(user, request);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@AuthenticationPrincipal User user, @PathVariable Long id,
                                   @Valid @RequestBody CategoryRequest request) {
        return service.update(user, id, request);
    }

    /** 204 No Content: success, and there is nothing to send back. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        service.delete(user, id);
    }
}
