package net.tfassbender.game.game;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class GameEventService {

    private static final Logger LOG = LoggerFactory.getLogger(GameEventService.class);

    // Map of gameId -> list of SSE connections
    private final Map<String, CopyOnWriteArrayList<SseEventSink>> connections = new ConcurrentHashMap<>();

    /**
     * Register a new SSE connection for a game
     */
    public void registerConnection(String gameId, SseEventSink eventSink) {
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
     * Broadcast an event to all connections for a game
     */
    public void broadcastEvent(String gameId, String eventType, Object data, Sse sse) {
        CopyOnWriteArrayList<SseEventSink> sinks = connections.get(gameId);
        if (sinks == null || sinks.isEmpty()) {
            LOG.debug("No active connections for game {}", gameId);
            return;
        }

        LOG.info("Broadcasting {} event to {} connections for game {}", eventType, sinks.size(), gameId);

        sinks.forEach(sink -> {
            if (!sink.isClosed()) {
                try {
                    sink.send(sse.newEventBuilder()
                            .name(eventType)
                            .data(data)
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
