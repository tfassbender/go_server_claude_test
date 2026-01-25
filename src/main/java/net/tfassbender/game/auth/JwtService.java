package net.tfassbender.game.auth;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class JwtService {

    @ConfigProperty(name = "jwt.expiration.hours", defaultValue = "24")
    int expirationHours;

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "go-game-server")
    String issuer;

    @ConfigProperty(name = "smallrye.jwt.sign.key")
    String signingKey;

    /**
     * Generate JWT token for authenticated user
     */
    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofHours(expirationHours));

        Set<String> roles = new HashSet<>();
        roles.add("User");

        return Jwt.issuer(issuer)
                .upn(username)
                .groups(roles)
                .expiresAt(expiry)
                .issuedAt(now)
                .signWithSecret(signingKey);
    }

    /**
     * Extract username from token (validation handled by Quarkus JWT extension)
     */
    public String extractUsername(String token) {
        // Token validation is handled by Quarkus @RolesAllowed and JWT filters
        // This method would be used if manual token parsing is needed
        return null; // Placeholder for now
    }
}
