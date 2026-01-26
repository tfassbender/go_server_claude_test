package net.tfassbender.game.auth;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class JwtService {

    private static final Logger LOG = LoggerFactory.getLogger(JwtService.class);

    @Inject
    JWTParser jwtParser;

    @ConfigProperty(name = "jwt.expiration.hours", defaultValue = "24")
    int expirationHours;

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "go-game-server")
    String issuer;

    @ConfigProperty(name = "smallrye.jwt.sign.key")
    String signingKeyBase64;

    /**
     * Generate JWT token for authenticated user
     */
    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofHours(expirationHours));

        Set<String> roles = new HashSet<>();
        roles.add("User");

        // Decode the base64 key to get raw bytes, matching how the JWK verification key is interpreted
        byte[] keyBytes = Base64.getDecoder().decode(signingKeyBase64);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

        return Jwt.issuer(issuer)
                .upn(username)
                .groups(roles)
                .expiresAt(expiry)
                .issuedAt(now)
                .sign(secretKey);
    }

    /**
     * Validate a JWT token and extract the username.
     * Used for SSE connections where the token is passed as a query parameter.
     *
     * @param token the JWT token string
     * @return Optional containing the username if valid, empty if invalid
     */
    public Optional<String> validateTokenAndGetUsername(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }

        try {
            JsonWebToken jwt = jwtParser.parse(token);
            String username = jwt.getName();
            if (username != null && !username.isEmpty()) {
                LOG.debug("Token validated for user: {}", username);
                return Optional.of(username);
            }
        } catch (ParseException e) {
            LOG.debug("Token validation failed: {}", e.getMessage());
        }

        return Optional.empty();
    }
}
