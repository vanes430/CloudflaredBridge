package github.vanes430.cloudflaredbridge.spigot;

import github.vanes430.cloudflaredbridge.common.BridgeConstants;
import github.vanes430.cloudflaredbridge.common.CloudflaredManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Paths;

public class CloudflaredBridgeSpigot extends JavaPlugin {

    private CloudflaredManager manager;

    @Override
    public void onEnable() {
        SpigotBridgeLogger logger = new SpigotBridgeLogger();
        logger.info("§aSpigot enabled!");

        // Use 'cloudflared' folder in server root
        manager = new CloudflaredManager(Paths.get("cloudflared"), logger);
        
        // Initialize async to avoid blocking main thread on network calls
        new Thread(() -> {
            manager.init();
        }).start();

        getCommand("cloudflared").setExecutor(new CloudflaredCommandSpigot(manager));
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.stop();
        }
    }
}
