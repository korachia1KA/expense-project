package com.portfolio.expensetracker.security;

import com.portfolio.expensetracker.user.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Creates and validates JSON Web Tokens (JWTs).
 *
 * <p><b>What is a JWT?</b> Three Base64URL strings joined by dots: {@code header.payload.signature}.
 * The payload holds "claims" such as {@code sub} (subject = the user's email), {@code iat}
 * (issued at) and {@code exp} (expires). The signature is an HMAC-SHA hash of header+payload computed
 * with a secret key only the server knows. Anyone can READ a JWT (it is not encrypted, so never put
 * secrets in it), but nobody can CHANGE it without invalidating the signature.
 *
 * <p><b>Where it fits:</b> {@code AuthService} calls {@link #generateToken} after a successful
 * login/registration; {@code JwtAuthenticationFilter} calls {@link #extractEmail} on every request.
 *
 * <p>{@code @Service} registers this class as a Spring bean (a singleton managed by Spring), so it can
 * be injected into other classes through their constructors.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    /**
     * {@code @Value("${...}")} injects values from application.yml (or env vars that override them).
     *
     * @param base64Secret the signing key, Base64 encoded (must decode to at least 256 bits)
     * @param expirationMs token lifetime in milliseconds
     */
    public JwtService(@Value("${app.jwt.secret}") String base64Secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // hmacShaKeyFor throws at startup if the key is too short: failing fast beats weak security.
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    /** Builds a signed token whose subject is the user's email. The user's name is added for convenience. */
    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("name", user.getName())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)            // JJWT picks the strongest HMAC algorithm the key length allows:
                                          // ≥256 bits → HS256, ≥384 → HS384 (our dev key), ≥512 → HS512
                .compact();               // serialises to the "xxx.yyy.zzz" string
    }

    /**
     * Verifies the signature and expiry, then returns the subject (email).
     *
     * @throws JwtException if the token was tampered with, is expired, or is malformed
     */
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
