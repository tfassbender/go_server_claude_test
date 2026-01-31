package net.tfassbender.game.ai.gtp;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Manages communication with a GNU Go process via the Go Text Protocol (GTP).
 * This client maintains a single persistent process and synchronizes all commands.
 */
@ApplicationScoped
public class GtpClient {
    private static final Logger LOG = LoggerFactory.getLogger(GtpClient.class);

    @ConfigProperty(name = "ai.gnugo.enabled", defaultValue = "false")
    boolean gnugoEnabled;

    @ConfigProperty(name = "ai.gnugo.base.dir")
    String gnugoBaseDir;

    @ConfigProperty(name = "ai.gnugo.executable.name.windows")
    String windowsExecutable;

    @ConfigProperty(name = "ai.gnugo.executable.name.linux")
    String linuxExecutable;

    @ConfigProperty(name = "ai.gtp.command.timeout", defaultValue = "30000")
    long commandTimeout;

    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private BufferedReader errorReader;
    private final Object lock = new Object();

    @PostConstruct
    public void start() {
        if (!gnugoEnabled) {
            LOG.info("GNU Go AI is disabled");
            return;
        }

        try {
            LOG.info("Starting GNU Go process...");

            String executable = System.getProperty("os.name").toLowerCase().contains("win")
                ? windowsExecutable
                : linuxExecutable;

            Path gnugoBasePath = Paths.get(gnugoBaseDir).toAbsolutePath().normalize();
            Path gnugoPath = gnugoBasePath.resolve(executable).normalize();

            LOG.info("GNU Go base directory: {}", gnugoBasePath);
            LOG.info("GNU Go executable path: {}", gnugoPath);

            ProcessBuilder pb = new ProcessBuilder(
                gnugoPath.toString(),  // Use full absolute path to executable
                "--mode", "gtp"
            );
            pb.directory(gnugoBasePath.toFile());  // Set working directory for DLLs

            LOG.debug("ProcessBuilder command: {} in directory: {}", pb.command(), pb.directory());

            process = pb.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            LOG.info("GNU Go process started successfully, PID: {}", process.pid());

            // Give process a moment to initialize
            Thread.sleep(500);

            // Check if process is still alive
            if (!process.isAlive()) {
                LOG.error("GNU Go process exited immediately with exit code: {}", process.exitValue());
                logErrorStream();
                return;
            }

            // Test connection
            GtpResponse nameResponse = sendCommand("name");
            if (nameResponse.isSuccess()) {
                LOG.info("GNU Go connected: {}", nameResponse.getResult());
            } else {
                LOG.error("Failed to connect to GNU Go: {}", nameResponse.getError());
                logErrorStream();
            }
        } catch (IOException e) {
            LOG.error("Failed to start GNU Go process", e);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while starting GNU Go process", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sends a GTP command and waits for the response.
     * Thread-safe: only one command can be executed at a time.
     */
    public GtpResponse sendCommand(String command) {
        if (!gnugoEnabled || process == null || !process.isAlive()) {
            return GtpResponse.error("GNU Go process not available");
        }

        synchronized (lock) {
            try {
                LOG.debug("Sending GTP command: {}", command);
                writer.write(command + "\n");
                writer.flush();

                return parseResponse();
            } catch (IOException e) {
                LOG.error("Error sending GTP command: {}", command, e);
                return GtpResponse.error("Communication error: " + e.getMessage());
            }
        }
    }

    /**
     * Parses a GTP response in the format:
     * = <result>
     * or
     * ? <error>
     */
    private GtpResponse parseResponse() throws IOException {
        StringBuilder response = new StringBuilder();
        String line;

        // Read first line to determine success/failure
        line = reader.readLine();
        if (line == null) {
            return GtpResponse.error("No response from GNU Go");
        }

        boolean success = line.startsWith("=");
        if (!success && !line.startsWith("?")) {
            return GtpResponse.error("Invalid GTP response format: " + line);
        }

        // First line contains either "= result" or "? error"
        String firstLineContent = line.substring(1).trim();
        if (!firstLineContent.isEmpty()) {
            response.append(firstLineContent);
        }

        // Read until we hit a blank line (end of response)
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                break;
            }
            if (!response.isEmpty()) {
                response.append("\n");
            }
            response.append(line.trim());
        }

        String result = response.toString();
        LOG.debug("Received GTP response: {}", success ? "= " + result : "? " + result);

        return success ? GtpResponse.success(result) : GtpResponse.error(result);
    }

    /**
     * Logs any error output from the GNU Go process.
     */
    private void logErrorStream() {
        try {
            if (errorReader != null && errorReader.ready()) {
                StringBuilder errorOutput = new StringBuilder("GNU Go error output:\n");
                String line;
                while (errorReader.ready() && (line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
                LOG.error(errorOutput.toString());
            }
        } catch (IOException e) {
            LOG.warn("Could not read error stream", e);
        }
    }

    /**
     * Checks if the GNU Go process is alive and responsive.
     */
    public boolean isHealthy() {
        if (!gnugoEnabled || process == null || !process.isAlive()) {
            return false;
        }

        GtpResponse response = sendCommand("name");
        return response.isSuccess();
    }

    @PreDestroy
    public void shutdown() {
        if (!gnugoEnabled || process == null) {
            return;
        }

        LOG.info("Shutting down GNU Go process...");
        synchronized (lock) {
            try {
                if (writer != null) {
                    writer.write("quit\n");
                    writer.flush();
                    writer.close();
                }
                if (reader != null) {
                    reader.close();
                }
                if (errorReader != null) {
                    errorReader.close();
                }
            } catch (IOException e) {
                LOG.warn("Error during GNU Go shutdown", e);
            }

            try {
                boolean exited = process.waitFor(5, TimeUnit.SECONDS);
                if (!exited) {
                    LOG.warn("GNU Go did not exit gracefully, forcing shutdown");
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for GNU Go to exit", e);
                Thread.currentThread().interrupt();
            }
        }
        LOG.info("GNU Go process shut down");
    }
}
