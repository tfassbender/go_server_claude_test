package net.tfassbender.game.ai;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import net.tfassbender.game.auth.PasswordHasher;
import net.tfassbender.game.user.User;
import net.tfassbender.game.user.UserRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

/**
 * Initializes AI bot user accounts on server startup.
 * Creates bot users with isBot flag set to true.
 */
@ApplicationScoped
public class AiBotInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(AiBotInitializer.class);

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordHasher passwordHasher;

    @ConfigProperty(name = "ai.gnugo.enabled", defaultValue = "false")
    boolean gnugoEnabled;

    @ConfigProperty(name = "ai.bots.config")
    String botsConfig;

    /**
     * Creates AI bot user accounts on server startup if they don't exist.
     */
    void onStart(@Observes StartupEvent event) {
        if (!gnugoEnabled) {
            LOG.info("GNU Go AI is disabled, skipping bot initialization");
            return;
        }

        LOG.info("Initializing GNU Go bot users...");

        String[] botEntries = botsConfig.split(",");
        int created = 0;
        int existing = 0;

        for (String entry : botEntries) {
            String[] parts = entry.trim().split(":");
            if (parts.length != 2) {
                LOG.warn("Invalid bot config entry: {}", entry);
                continue;
            }

            String botUsername = parts[0].trim();

            try {
                if (userRepository.exists(botUsername)) {
                    // Bot already exists, ensure isBot flag is set
                    var userOpt = userRepository.findByUsername(botUsername);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        if (!user.isBot) {
                            user.isBot = true;
                            userRepository.save(user);
                            LOG.info("Updated existing user {} to bot status", botUsername);
                        }
                    }
                    existing++;
                } else {
                    // Create new bot user
                    createBotUser(botUsername);
                    created++;
                }
            } catch (IOException e) {
                LOG.error("Failed to initialize bot user: {}", botUsername, e);
            }
        }

        LOG.info("AI bot initialization complete: {} created, {} existing", created, existing);
    }

    /**
     * Creates a new bot user account with a random password.
     * Bot users cannot log in via the UI, so the password is not important.
     */
    private void createBotUser(String username) throws IOException {
        // Generate a random secure password (bots don't need to log in)
        String randomPassword = UUID.randomUUID().toString();
        String passwordHash = passwordHasher.hashPassword(randomPassword);

        User botUser = new User(username, passwordHash);
        botUser.isBot = true;

        userRepository.save(botUser);
        LOG.info("Created AI bot user: {}", username);
    }
}
