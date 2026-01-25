package net.tfassbender.game.user;

import net.tfassbender.game.auth.PasswordHasher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

@ApplicationScoped
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordHasher passwordHasher;

    /**
     * Register a new user
     */
    public User registerUser(String username, String password) throws IOException {
        // Validate username
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (username.length() < 3 || username.length() > 20) {
            throw new IllegalArgumentException("Username must be between 3 and 20 characters");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores");
        }

        // Validate password
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        // Check if username already exists
        if (userRepository.exists(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Create and save user
        String passwordHash = passwordHasher.hashPassword(password);
        User user = new User(username, passwordHash);
        userRepository.save(user);

        LOG.info("Registered new user: {}", username);
        return user;
    }

    /**
     * Authenticate user with username and password
     */
    public Optional<User> authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            LOG.debug("User not found: {}", username);
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!passwordHasher.verifyPassword(password, user.getPasswordHash())) {
            LOG.debug("Invalid password for user: {}", username);
            return Optional.empty();
        }

        LOG.info("User authenticated: {}", username);
        return Optional.of(user);
    }

    /**
     * Get user by username
     */
    public Optional<User> getUser(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Update user statistics
     */
    public void updateStatistics(String username, boolean won) throws IOException {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();
        user.statistics.gamesPlayed++;
        if (won) {
            user.statistics.wins++;
        } else {
            user.statistics.losses++;
        }

        userRepository.save(user);
        LOG.info("Updated statistics for user: {}", username);
    }
}
