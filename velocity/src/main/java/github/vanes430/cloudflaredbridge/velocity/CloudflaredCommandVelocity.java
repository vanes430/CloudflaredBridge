package github.vanes430.cloudflaredbridge.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import github.vanes430.cloudflaredbridge.common.BridgeConstants;
import github.vanes430.cloudflaredbridge.common.CloudflaredManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CloudflaredCommandVelocity implements SimpleCommand {

    private final CloudflaredManager manager;

    public CloudflaredCommandVelocity(CloudflaredManager manager) {
        this.manager = manager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("cloudflaredbridge.admin")) {
            source.sendMessage(LegacyComponentSerializer.legacySection().deserialize(BridgeConstants.PREFIX + "§cNo permission."));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(LegacyComponentSerializer.legacySection().deserialize(BridgeConstants.PREFIX + "Usage: /cloudflared <start|stop>"));
            return;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("start")) {
            int delay = 10;
            for (String arg : args) {
                if (arg.startsWith("--delay=")) {
                    try {
                        delay = Integer.parseInt(arg.substring(8));
                    } catch (NumberFormatException e) {
                        source.sendMessage(LegacyComponentSerializer.legacySection().deserialize(BridgeConstants.PREFIX + "§cInvalid delay format. Using default 10s."));
                    }
                }
            }
            final int finalDelay = delay;
            source.sendMessage(LegacyComponentSerializer.legacySection().deserialize(BridgeConstants.PREFIX + "§eStarting Cloudflared processes (delay=" + finalDelay + "s)..."));
            CompletableFuture.runAsync(() -> manager.start(finalDelay));
        } else if (sub.equals("stop")) {
            source.sendMessage(LegacyComponentSerializer.legacySection().deserialize(BridgeConstants.PREFIX + "§eStopping Cloudflared processes..."));
            CompletableFuture.runAsync(manager::stop);
        } else {
             source.sendMessage(LegacyComponentSerializer.legacySection().deserialize(BridgeConstants.PREFIX + "Usage: /cloudflared <start|stop>"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of("start", "stop");
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("cloudflaredbridge.admin");
    }
}
