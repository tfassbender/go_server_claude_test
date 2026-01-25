package net.tfassbender.game.game;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Path("/api/games/{gameId}/events")
@RolesAllowed("User")
public class GameEventResource {

    private static final Logger LOG = LoggerFactory.getLogger(GameEventResource.class);

    @Inject
    GameService gameService;

    @Inject
    GameEventService eventService;

    @Inject
    JsonWebToken jwt;

    /**
     * Server-Sent Events endpoint for game updates
     */
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void streamGameEvents(
            @PathParam("gameId") String gameId,
            @Context SseEventSink eventSink,
            @Context Sse sse) {

        String username = jwt.getName();

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

        // Register connection
        eventService.registerConnection(gameId, eventSink);
        LOG.info("SSE stream opened for game {} by user {}", gameId, username);

        // Send initial connection event
        try {
            eventSink.send(sse.newEventBuilder()
                    .name("connected")
                    .data("SSE connection established")
                    .build());
        } catch (Exception e) {
            LOG.error("Error sending connection event", e);
        }

        // Handle connection close
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            eventService.unregisterConnection(gameId, eventSink);
            eventSink.close();
        }));
    }
}
