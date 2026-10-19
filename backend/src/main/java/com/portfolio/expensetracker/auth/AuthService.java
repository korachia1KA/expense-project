package com.portfolio.expensetracker.auth;

import com.portfolio.expensetracker.auth.dto.AuthResponse;
import com.portfolio.expensetracker.auth.dto.LoginRequest;
import com.portfolio.expensetracker.auth.dto.RegisterRequest;
import com.portfolio.expensetracker.auth.dto.UserResponse;
import com.portfolio.expensetracker.category.CategoryService;
import com.portfolio.expensetracker.common.ConflictException;
import com.portfolio.expensetracker.security.JwtService;
import com.portfolio.expensetracker.user.User;
import com.portfolio.expensetracker.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login.
 *
 * <p><b>Request flow:</b> {@code AuthController} → {@code AuthService} → {@code UserRepository}
 * (+ {@code CategoryService} for default categories) → {@code JwtService} creates the token.
 *
 * <p>Dependencies arrive through the constructor ("constructor injection"). Spring sees the single
 * constructor and passes in the matching beans. Fields can then be {@code final}, and unit tests can
 * simply call {@code new AuthService(mockRepo, ...)}.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategoryService categoryService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       CategoryService categoryService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.categoryService = categoryService;
    }

    /**
     * Creates the account, gives it the default categories and returns a token (so the user is
     * logged in straight away).
     *
     * <p>{@code @Transactional} wraps the method in one database transaction: either the user AND
     * all default categories are saved, or (if anything throws) nothing is. No half-created accounts.
     *
     * @throws ConflictException (→ 409) if the email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Normalise the email so "Jane@Example.com" and "jane@example.com" are the same account.
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        // encode() hashes the password with BCrypt (random salt included in the result).
        User user = users.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password())));
        categoryService.createDefaults(user);
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    /**
     * Checks the credentials and returns a fresh token.
     *
     * <p>{@code readOnly = true} is a hint that lets Hibernate skip dirty-checking (a small speed-up)
     * and documents that this method never writes.
     *
     * @throws BadCredentialsException (→ 401) with the SAME message whether the email is unknown or the
     *         password is wrong, so attackers cannot discover which emails are registered.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                // matches() hashes the attempt with the stored salt and compares in constant time.
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }
}
