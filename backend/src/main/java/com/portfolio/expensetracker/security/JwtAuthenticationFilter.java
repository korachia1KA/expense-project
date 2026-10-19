package com.portfolio.expensetracker.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Turns an "Authorization: Bearer &lt;jwt&gt;" header into a logged-in user for ONE request.
 *
 * <p><b>Where it fits:</b> every HTTP request passes through Spring Security's filter chain BEFORE it
 * reaches a controller. {@code SecurityConfig} inserts this filter into that chain. If the token is
 * valid, we put the {@code User} into the {@link SecurityContextHolder}; Spring Security then lets the
 * request through to protected endpoints, and controllers can read the user via
 * {@code @AuthenticationPrincipal}.
 *
 * <p>If the header is missing or the token is invalid we do NOT reject the request here. We simply
 * leave it anonymous; Spring Security's authorization rules later answer 401 for protected URLs,
 * while public URLs (login/register) still work. One place decides access, one place reads tokens.
 *
 * <p>{@link OncePerRequestFilter} guarantees the filter runs exactly once per request (servlet
 * forwards/includes could otherwise run it twice). It is not a {@code @Component} on purpose:
 * Spring Boot would otherwise also register it as a global servlet filter, running it twice.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 1. Verify signature + expiry and read the email from the token.
                String email = jwtService.extractEmail(header.substring(BEARER_PREFIX.length()));
                // 2. Load the current user from the DB (so deleted accounts stop working immediately).
                UserDetails user = userDetailsService.loadUserByUsername(email);
                // 3. Mark the request as authenticated. Credentials are null: the token already proved identity.
                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
                // Invalid/expired token or unknown user: continue anonymously -> protected URLs return 401.
            }
        }
        // Always pass the request on to the next filter (and eventually the controller).
        chain.doFilter(request, response);
    }
}
