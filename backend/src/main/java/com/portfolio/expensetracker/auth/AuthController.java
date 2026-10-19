package com.portfolio.expensetracker.auth;

import com.portfolio.expensetracker.auth.dto.AuthResponse;
import com.portfolio.expensetracker.auth.dto.LoginRequest;
import com.portfolio.expensetracker.auth.dto.RegisterRequest;
import com.portfolio.expensetracker.auth.dto.UserResponse;
import com.portfolio.expensetracker.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP endpoints for authentication, under {@code /api/auth}.
 *
 * <p>{@code @RestController} = {@code @Controller} + {@code @ResponseBody}: return values are written
 * to the response as JSON (by Jackson) instead of being treated as HTML view names.
 * {@code @RequestMapping("/api/auth")} prefixes every route in the class.
 *
 * <p>Controllers stay thin on purpose: they translate HTTP to method calls and back, and delegate
 * all logic to the service. That keeps business rules testable without HTTP.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * {@code POST /api/auth/register} → 201 Created + token.
     * {@code @RequestBody} parses the JSON body into the record; {@code @Valid} runs its validation
     * annotations first, and if any fail the method is never called (400 is returned instead).
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** {@code POST /api/auth/login} → 200 + token, or 401 on wrong credentials. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * {@code GET /api/auth/me} → the logged-in user. {@code @AuthenticationPrincipal} injects the
     * {@link User} that {@code JwtAuthenticationFilter} placed in the security context.
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.from(user);
    }
}
