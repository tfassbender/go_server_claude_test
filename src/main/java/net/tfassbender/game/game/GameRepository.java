package net.tfassbender.game.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class GameRepository {

    private static final Logger LOG = LoggerFactory.getLogger(GameRepository.class);

    @ConfigProperty(name = "app.data.directory", defaultValue = "data")
    String dataDirectory;

    private final ObjectMapper objectMapper;

    public GameRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Save game to file system
     */
    public void save(Game game) throws IOException {
        Path gamesDir = getGameDirectory(game.status);
        Files.createDirectories(gamesDir);

        Path gameFile = gamesDir.resolve(game.id + ".json");

        // Atomic write to prevent corruption
        Path tempFile = Files.createTempFile(gamesDir, game.id, ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), game);
        Files.move(tempFile, gameFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        LOG.debug("Saved game: {} (status: {})", game.id, game.status);
    }

    /**
     * Find game by ID (search all status directories)
     */
    public Optional<Game> findById(String gameId) {
        String[] statuses = {"pending", "active", "completed"};

        for (String status : statuses) {
            Path gameFile = getGameDirectory(status).resolve(gameId + ".json");
            if (Files.exists(gameFile)) {
                try {
                    Game game = objectMapper.readValue(gameFile.toFile(), Game.class);
                    return Optional.of(game);
                } catch (IOException e) {
                    LOG.error("Error reading game file: {}", gameId, e);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Find all games for a user
     */
    public List<Game> findByUser(String username, String status) {
        List<Game> userGames = new ArrayList<>();

        try {
            Path gamesDir = getGameDirectory(status);
            if (!Files.exists(gamesDir)) {
                return userGames;
            }

            try (Stream<Path> paths = Files.list(gamesDir)) {
                List<Path> gameFiles = paths
                        .filter(path -> path.toString().endsWith(".json"))
                        .collect(Collectors.toList());

                for (Path gameFile : gameFiles) {
                    try {
                        Game game = objectMapper.readValue(gameFile.toFile(), Game.class);
                        if (game.blackPlayer.equals(username) || game.whitePlayer.equals(username)) {
                            userGames.add(game);
                        }
                    } catch (IOException e) {
                        LOG.error("Error reading game file: {}", gameFile.getFileName(), e);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Error listing games for user: {}", username, e);
        }

        // Sort by last move time, most recent first
        userGames.sort((g1, g2) -> g2.lastMoveAt.compareTo(g1.lastMoveAt));

        return userGames;
    }

    /**
     * Find all games for a user (all statuses)
     */
    public List<Game> findAllByUser(String username) {
        List<Game> allGames = new ArrayList<>();
        allGames.addAll(findByUser(username, "pending"));
        allGames.addAll(findByUser(username, "active"));
        allGames.addAll(findByUser(username, "completed"));
        return allGames;
    }

    /**
     * Move game file when status changes
     */
    public void moveGameFile(Game game, String oldStatus) throws IOException {
        Path oldFile = getGameDirectory(oldStatus).resolve(game.id + ".json");
        if (Files.exists(oldFile)) {
            Files.delete(oldFile);
        }
        save(game);
    }

    /**
     * Delete game file
     */
    public void delete(String gameId) throws IOException {
        String[] statuses = {"pending", "active", "completed"};

        for (String status : statuses) {
            Path gameFile = getGameDirectory(status).resolve(gameId + ".json");
            if (Files.exists(gameFile)) {
                Files.delete(gameFile);
                LOG.info("Deleted game: {}", gameId);
                return;
            }
        }
    }

    /**
     * Get directory path for games with specific status
     */
    private Path getGameDirectory(String status) {
        return Paths.get(dataDirectory, "games", status);
    }
}
