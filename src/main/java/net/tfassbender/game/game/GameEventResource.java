package net.tfassbender.game.game;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import net.tfassbender.game.auth.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Path("/api/games/{gameId}/events")
public class GameEventResource {

    private static final Logger LOG = LoggerFactory.getLogger(GameEventResource.class);

    @Inject
    GameService gameService;

    @Inject
    GameEventService eventService;

    @Inject
    JwtService jwtService;

    /**
     * Server-Sent Events endpoint for game updates.
     * Accepts JWT token as query parameter since EventSource doesn't support custom headers.
     */
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void streamGameEvents(
            @PathParam("gameId") String gameId,
            @QueryParam("token") String token,
            @Context SseEventSink eventSink,
            @Context Sse sse) {

        // Validate token from query parameter
        Optional<String> usernameOpt = jwtService.validateTokenAndGetUsername(token);
        if (usernameOpt.isEmpty()) {
            LOG.error("Invalid or missing token for SSE connection to game {}", gameId);
            eventSink.close();
            return;
        }
        String username = usernameOpt.get();

        // Validate game exists and user is a player
        Optional<Game> gameOpt = gameService.getGame(gameId);
        if (gameOpt.isEmpty()) {
            LOG.error("Game not found: {}", gameId);
            eventSink.close();
            return;
        }

        Game game = gameOpt.get();
        if (!game.isPlayer(username)) {
            LOG.error("User {} is not a player in game {}", username, gameId);
            eventSink.close();
            return;
        }

        // Register connection (also stores Sse instance for AI move broadcasts)
        eventService.registerConnection(gameId, eventSink, sse);
        LOG.info("SSE stream opened for game {} by user {}", gameId, username);

        // Send initial connection event and handle connection lifecycle
        eventSink.send(sse.newEventBuilder()
                .name("connected")
                .data("SSE connection established")
                .build())
                .whenComplete((result, error) -> {
                    if (error != null) {
                        LOG.debug("SSE connection closed for game {} user {}: {}", gameId, username, error.getMessage());
                        eventService.unregisterConnection(gameId, eventSink);
                    }
                });

        // Start a keep-alive mechanism to detect disconnections
        startKeepAlive(gameId, username, eventSink, sse);
    }

    /**
     * Sends periodic keep-alive comments to detect when connection is closed.
     * SSE connections can silently die, so we need to actively check.
     */
    private void startKeepAlive(String gameId, String username, SseEventSink eventSink, Sse sse) {
        Thread keepAliveThread = new Thread(() -> {
            while (!eventSink.isClosed()) {
                try {
                    Thread.sleep(30000); // Send keep-alive every 30 seconds
                    if (!eventSink.isClosed()) {
                        eventSink.send(sse.newEventBuilder()
                                .comment("keep-alive")
                                .build())
                                .whenComplete((result, error) -> {
                                    if (error != null) {
                                        LOG.debug("Keep-alive failed for game {} user {}, connection closed", gameId, username);
                                        eventService.unregisterConnection(gameId, eventSink);
                                    }
                                });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            eventService.unregisterConnection(gameId, eventSink);
            LOG.debug("Keep-alive thread ended for game {} user {}", gameId, username);
        });
        keepAliveThread.setDaemon(true);
        keepAliveThread.start();
    }
}
