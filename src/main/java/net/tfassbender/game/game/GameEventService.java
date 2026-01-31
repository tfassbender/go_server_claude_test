package net.tfassbender.game.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class GameEventService {

    private static final Logger LOG = LoggerFactory.getLogger(GameEventService.class);

    @Inject
    ObjectMapper objectMapper;

    // Map of gameId -> list of SSE connections
    private final Map<String, CopyOnWriteArrayList<SseEventSink>> connections = new ConcurrentHashMap<>();

    // Store the Sse instance for use when broadcasting from non-REST contexts (e.g., AI moves)
    private final AtomicReference<Sse> sseInstance = new AtomicReference<>();

    /**
     * Register a new SSE connection for a game
     */
    public void registerConnection(String gameId, SseEventSink eventSink, Sse sse) {
        // Store the Sse instance for later use (e.g., AI moves)
        sseInstance.compareAndSet(null, sse);

        connections.computeIfAbsent(gameId, k -> new CopyOnWriteArrayList<>()).add(eventSink);
        LOG.info("SSE connection registered for game {} (total: {})", gameId, connections.get(gameId).size());
    }

    /**
     * Unregister an SSE connection
     */
    public void unregisterConnection(String gameId, SseEventSink eventSink) {
        CopyOnWriteArrayList<SseEventSink> sinks = connections.get(gameId);
        if (sinks != null) {
            sinks.remove(eventSink);
            if (sinks.isEmpty()) {
                connections.remove(gameId);
            }
            LOG.info("SSE connection unregistered for game {}", gameId);
        }
    }

    /**
     * Broadcast an event to all connections for a game (uses stored Sse instance)
     * This is used when broadcasting from non-REST contexts like AI moves.
     */
    public void broadcastEvent(String gameId, String eventType, Object data) {
        Sse sse = sseInstance.get();
        if (sse == null) {
            LOG.warn("Cannot broadcast event: no Sse instance available. Has any client connected yet?");
            return;
        }
        broadcastEvent(gameId, eventType, data, sse);
    }

    /**
     * Broadcast an event to all connections for a game
     */
    public void broadcastEvent(String gameId, String eventType, Object data, Sse sse) {
        CopyOnWriteArrayList<SseEventSink> sinks = connections.get(gameId);
        if (sinks == null || sinks.isEmpty()) {
            LOG.debug("No active connections for game {}", gameId);
            return;
        }

        LOG.info("Broadcasting {} event to {} connections for game {}", eventType, sinks.size(), gameId);

        // Serialize data to JSON string
        String jsonData;
        try {
            jsonData = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            LOG.error("Error serializing SSE event data to JSON", e);
            return;
        }

        sinks.forEach(sink -> {
            if (!sink.isClosed()) {
                try {
                    sink.send(sse.newEventBuilder()
                            .name(eventType)
                            .data(jsonData)
                            .build());
                } catch (Exception e) {
                    LOG.error("Error sending SSE event to sink", e);
                    unregisterConnection(gameId, sink);
                }
            } else {
                unregisterConnection(gameId, sink);
            }
        });
    }

    /**
     * Get number of active connections for a game
     */
    public int getConnectionCount(String gameId) {
        CopyOnWriteArrayList<SseEventSink> sinks = connections.get(gameId);
        return sinks != null ? sinks.size() : 0;
    }
}
