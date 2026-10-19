package com.portfolio.expensetracker.auth.dto;

import com.portfolio.expensetracker.user.User;

/**
 * Public view of a user. Note what is NOT here: the password hash. Returning the {@code User}
 * entity directly would risk leaking it (and every future sensitive field) in JSON.
 */
public record UserResponse(Long id, String name, String email) {

    /** Maps the entity to the DTO. Static factory methods like this keep controllers tidy. */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
