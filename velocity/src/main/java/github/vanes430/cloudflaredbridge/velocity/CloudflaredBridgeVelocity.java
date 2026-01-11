package github.vanes430.cloudflaredbridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import github.vanes430.cloudflaredbridge.common.CloudflaredManager;

import java.nio.file.Paths;

@Plugin(
    id = "cloudflaredbridge",
    name = "CloudflaredBridge",
    version = "1.0.0",
    authors = {"vanes430"}
)
public class CloudflaredBridgeVelocity {

    private final ProxyServer server;
    private CloudflaredManager manager;

    @Inject
    public CloudflaredBridgeVelocity(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        VelocityBridgeLogger logger = new VelocityBridgeLogger(server);
        logger.info("§aVelocity enabled!");

        manager = new CloudflaredManager(Paths.get("cloudflared"), logger);
        
        // Async init
        server.getScheduler().buildTask(this, manager::init).schedule();
        
        server.getCommandManager().register("cloudflared", new CloudflaredCommandVelocity(manager));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (manager != null) {
            manager.stop();
        }
    }
}
