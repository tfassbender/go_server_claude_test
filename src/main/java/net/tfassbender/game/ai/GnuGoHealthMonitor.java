package net.tfassbender.game.ai;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.tfassbender.game.ai.gtp.GtpClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Monitors the health of the GNU Go process and attempts restarts on failure.
 */
@ApplicationScoped
public class GnuGoHealthMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(GnuGoHealthMonitor.class);

    @Inject
    GtpClient gtpClient;

    @ConfigProperty(name = "ai.gnugo.enabled", defaultValue = "false")
    boolean gnugoEnabled;

    @ConfigProperty(name = "ai.gtp.process.restart.maxAttempts", defaultValue = "3")
    int maxRestartAttempts;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger restartAttempts = new AtomicInteger(0);

    /**
     * Runs every 60 seconds to check GNU Go health.
     */
    @Scheduled(every = "60s")
    void checkHealth() {
        if (!gnugoEnabled) {
            return;
        }

        LOG.debug("Checking GNU Go health...");

        boolean healthy = gtpClient.isHealthy();

        if (healthy) {
            // Reset failure counters on successful check
            int previousFailures = consecutiveFailures.getAndSet(0);
            if (previousFailures > 0) {
                LOG.info("GNU Go health restored after {} consecutive failures", previousFailures);
            }
            restartAttempts.set(0);
        } else {
            int failures = consecutiveFailures.incrementAndGet();
            LOG.warn("GNU Go health check failed (consecutive failures: {})", failures);

            // Attempt restart after 2 consecutive failures
            if (failures >= 2) {
                attemptRestart();
            }
        }
    }

    /**
     * Attempts to restart the GNU Go process.
     */
    private void attemptRestart() {
        int attempts = restartAttempts.incrementAndGet();

        if (attempts > maxRestartAttempts) {
            LOG.error("GNU Go restart attempts exceeded maximum ({}), giving up", maxRestartAttempts);
            return;
        }

        LOG.warn("Attempting to restart GNU Go (attempt {} of {})", attempts, maxRestartAttempts);

        try {
            // Shutdown existing process
            gtpClient.shutdown();

            // Wait a bit before restarting
            Thread.sleep(2000);

            // Start new process
            gtpClient.start();

            LOG.info("GNU Go restart attempt completed");

        } catch (Exception e) {
            LOG.error("Failed to restart GNU Go", e);
        }
    }
}
