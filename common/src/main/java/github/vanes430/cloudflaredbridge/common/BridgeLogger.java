package github.vanes430.cloudflaredbridge.common;

public interface BridgeLogger {
    void info(String message);
    void warning(String message);
    void severe(String message);
}
