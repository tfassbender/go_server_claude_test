package net.tfassbender.game;

import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.io.InputStream;

import io.quarkus.vertx.http.runtime.filters.Filters;

@ApplicationScoped
public class SpaRoutingFilter {

    public void registerRoutes(@Observes Filters filters) {
        filters.register(this::rerouteToIndex, 1000);
    }

    private void rerouteToIndex(RoutingContext rc) {
        String path = rc.normalizedPath();

        // Let API requests, static assets, and Quarkus dev UI pass through
        if (path.startsWith("/api/") ||
            path.startsWith("/q/") ||
            path.contains(".") ||
            path.equals("/")) {
            rc.next();
            return;
        }

        // For SPA routes, serve index.html
        InputStream indexHtml = getClass().getResourceAsStream("/META-INF/resources/index.html");
        if (indexHtml != null) {
            try {
                byte[] content = indexHtml.readAllBytes();
                rc.response()
                    .putHeader("Content-Type", "text/html")
                    .end(io.vertx.core.buffer.Buffer.buffer(content));
            } catch (Exception e) {
                rc.next();
            }
        } else {
            rc.next();
        }
    }
}
