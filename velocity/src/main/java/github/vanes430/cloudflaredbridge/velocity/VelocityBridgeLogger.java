package github.vanes430.cloudflaredbridge.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import github.vanes430.cloudflaredbridge.common.BridgeConstants;
import github.vanes430.cloudflaredbridge.common.BridgeLogger;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class VelocityBridgeLogger implements BridgeLogger {

    private final ProxyServer server;

    public VelocityBridgeLogger(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void info(String message) {
        log(message);
    }

    @Override
    public void warning(String message) {
        log("§e" + message);
    }

    @Override
    public void severe(String message) {
        log("§c" + message);
    }

    private void log(String message) {
        server.getConsoleCommandSource().sendMessage(
            LegacyComponentSerializer.legacySection().deserialize(BridgeConstants.PREFIX + message)
        );
    }
}
