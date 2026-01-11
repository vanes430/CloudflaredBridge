package github.vanes430.cloudflaredbridge.spigot;

import github.vanes430.cloudflaredbridge.common.BridgeConstants;
import github.vanes430.cloudflaredbridge.common.BridgeLogger;
import org.bukkit.Bukkit;

public class SpigotBridgeLogger implements BridgeLogger {
    @Override
    public void info(String message) {
        Bukkit.getConsoleSender().sendMessage(BridgeConstants.PREFIX + message);
    }

    @Override
    public void warning(String message) {
        Bukkit.getConsoleSender().sendMessage(BridgeConstants.PREFIX + "§e" + message);
    }

    @Override
    public void severe(String message) {
        Bukkit.getConsoleSender().sendMessage(BridgeConstants.PREFIX + "§c" + message);
    }
}
