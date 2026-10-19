package com.portfolio.expensetracker.config;

import com.portfolio.expensetracker.security.JwtAuthenticationFilter;
import com.portfolio.expensetracker.security.JwtService;
import com.portfolio.expensetracker.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * All Spring Security configuration in one place: which URLs are public, how users are loaded,
 * how passwords are hashed and which browser origins may call the API.
 *
 * <p>{@code @Configuration} marks a class whose {@code @Bean} methods create objects that Spring
 * manages and injects wherever they are needed (for example {@link PasswordEncoder} is injected into
 * {@code AuthService}).
 *
 * <p><b>Authorization model:</b> this app only needs "is the caller logged in?". Ownership
 * ("is this transaction YOURS?") is enforced in the service layer by always querying with the
 * caller's user id. Bigger apps with roles would add method-level rules such as
 * {@code @PreAuthorize("hasRole('ADMIN')")} (enabled with {@code @EnableMethodSecurity}); we don't
 * need that here, so URL rules below are enough.
 */
@Configuration
public class SecurityConfig {

    /** URLs anyone may call without a token. Everything else requires authentication. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/register",
            "/api/auth/login",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/error"             // Spring's error page must be reachable or errors turn into 401s
    };

    /**
     * The security filter chain: an ordered list of filters every request passes through.
     * Each line of the builder below adds or tunes one of those filters.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   UserDetailsService userDetailsService) throws Exception {
        return http
                // CSRF attacks abuse cookies the browser sends automatically. We authenticate with a
                // header the JavaScript adds explicitly, so CSRF protection is unnecessary.
                .csrf(AbstractHttpConfigurer::disable)
                // Use the corsConfigurationSource bean below.
                .cors(Customizer.withDefaults())
                // The H2 console uses HTML frames; allow frames from the same origin (dev only).
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                // STATELESS: never create an HTTP session. Each request must carry its own JWT.
                // This lets us run several backend instances without sharing session storage.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                // By default Spring would answer unauthenticated calls with a login page/redirect.
                // An API should simply answer "401 Unauthorized" so the Angular app can react.
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Run our JWT filter before Spring's username/password filter so the user is known early.
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Tells Spring Security how to load a user by "username" (our email).
     * A lambda works because {@link UserDetailsService} has a single abstract method.
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    /**
     * BCrypt is a deliberately SLOW, salted hashing algorithm. Slow means brute-forcing leaked hashes
     * is expensive; salted means two users with the same password get different hashes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS (Cross-Origin Resource Sharing): browsers block JavaScript from calling a different origin
     * (scheme+host+port) unless that server explicitly allows it. In dev and Docker the frontend and API
     * share one origin through a proxy, so this is only a fallback for split deployments.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
