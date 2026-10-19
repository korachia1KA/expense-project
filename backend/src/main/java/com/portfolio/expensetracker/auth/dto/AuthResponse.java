package com.portfolio.expensetracker.auth.dto;

/**
 * Returned by login and register: {@code { "token": "eyJ...", "user": { "id": 1, ... } }}.
 * The Angular app stores the token and sends it back in the Authorization header on every request.
 */
public record AuthResponse(String token, UserResponse user) {
}
