package net.tfassbender.game.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class UserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepository.class);

    @ConfigProperty(name = "app.data.directory", defaultValue = "data")
    String dataDirectory;

    private final ObjectMapper objectMapper;

    public UserRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Save user to file system
     */
    public void save(User user) throws IOException {
        Path usersDir = Paths.get(dataDirectory, "users");
        Files.createDirectories(usersDir);

        Path userFile = usersDir.resolve(user.username + ".json");

        // Write to temp file first, then atomic move to prevent corruption
        Path tempFile = Files.createTempFile(usersDir, user.username, ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), user);
        Files.move(tempFile, userFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        LOG.info("Saved user: {}", user.username);
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        Path userFile = Paths.get(dataDirectory, "users", username + ".json");

        if (!Files.exists(userFile)) {
            return Optional.empty();
        }

        try {
            User user = objectMapper.readValue(userFile.toFile(), User.class);
            return Optional.of(user);
        } catch (IOException e) {
            LOG.error("Error reading user file: {}", username, e);
            return Optional.empty();
        }
    }

    /**
     * Check if user exists
     */
    public boolean exists(String username) {
        Path userFile = Paths.get(dataDirectory, "users", username + ".json");
        return Files.exists(userFile);
    }

    /**
     * Delete user (for testing/admin purposes)
     */
    public void delete(String username) throws IOException {
        Path userFile = Paths.get(dataDirectory, "users", username + ".json");
        Files.deleteIfExists(userFile);
        LOG.info("Deleted user: {}", username);
    }

    /**
     * Find all usernames, optionally filtered by search query
     */
    public List<String> findAllUsernames(String searchQuery) {
        Path usersDir = Paths.get(dataDirectory, "users");

        if (!Files.exists(usersDir)) {
            return Collections.emptyList();
        }

        try (Stream<Path> files = Files.list(usersDir)) {
            String query = searchQuery != null ? searchQuery.toLowerCase() : "";
            return files
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> {
                        String filename = path.getFileName().toString();
                        return filename.substring(0, filename.length() - 5); // Remove .json
                    })
                    .filter(username -> query.isEmpty() || username.toLowerCase().contains(query))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            LOG.error("Error listing users", e);
            return Collections.emptyList();
        }
    }
}
