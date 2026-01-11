package github.vanes430.cloudflaredbridge.spigot;

import github.vanes430.cloudflaredbridge.common.BridgeConstants;
import github.vanes430.cloudflaredbridge.common.CloudflaredManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CloudflaredCommandSpigot implements CommandExecutor {

    private final CloudflaredManager manager;

    public CloudflaredCommandSpigot(CloudflaredManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cloudflaredbridge.admin")) {
            sender.sendMessage(BridgeConstants.PREFIX + "§cNo permission.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(BridgeConstants.PREFIX + "Usage: /cloudflared <start|stop>");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("start")) {
            int delay = 10;
            for (String arg : args) {
                if (arg.startsWith("--delay=")) {
                    try {
                        delay = Integer.parseInt(arg.substring(8));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(BridgeConstants.PREFIX + "§cInvalid delay format. Using default 10s.");
                    }
                }
            }
            final int finalDelay = delay;
            sender.sendMessage(BridgeConstants.PREFIX + "§eStarting Cloudflared processes (delay=" + finalDelay + "s)...");
            new Thread(() -> manager.start(finalDelay)).start();
            return true;
        } else if (sub.equals("stop")) {
            sender.sendMessage(BridgeConstants.PREFIX + "§eStopping Cloudflared processes...");
            new Thread(manager::stop).start();
            return true;
        }

        sender.sendMessage(BridgeConstants.PREFIX + "Usage: /cloudflared <start|stop>");
        return true;
    }
}
