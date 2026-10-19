package com.portfolio.expensetracker.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Database access for {@link User}.
 *
 * <p>This is only an interface: Spring Data JPA generates the implementation at startup.
 * Extending {@code JpaRepository<User, Long>} gives save, findById, findAll, delete, count... for free.
 *
 * <p>Extra methods are "derived queries": Spring parses the method NAME and writes the SQL.
 * {@code findByEmailIgnoreCase} becomes {@code select ... from users where upper(email) = upper(?)}.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used at login and by the JWT filter. {@link Optional} forces callers to handle "not found". */
    Optional<User> findByEmailIgnoreCase(String email);

    /** Used at registration to reject duplicate emails with a 409 Conflict. */
    boolean existsByEmailIgnoreCase(String email);
}
