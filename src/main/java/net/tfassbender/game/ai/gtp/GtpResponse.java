package net.tfassbender.game.ai.gtp;

/**
 * Represents a response from a GTP (Go Text Protocol) command.
 */
public class GtpResponse {
    private final boolean success;
    private final String result;
    private final String error;

    public GtpResponse(boolean success, String result, String error) {
        this.success = success;
        this.result = result;
        this.error = error;
    }

    public static GtpResponse success(String result) {
        return new GtpResponse(true, result, null);
    }

    public static GtpResponse error(String error) {
        return new GtpResponse(false, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return success ? "Success: " + result : "Error: " + error;
    }
}
