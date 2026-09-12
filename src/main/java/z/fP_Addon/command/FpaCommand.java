package z.fP_Addon.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import z.fP_Addon.FP_Addon;
import z.fP_Addon.utils.HEXColor;

public class FpaCommand implements CommandExecutor {

    private final FP_Addon plugin;

    public FpaCommand(FP_Addon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, String[] args) {

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(HEXColor.colorize("&cИспользование: /fpa reload"));
            return true;
        }

        if (!sender.hasPermission("fpa.admin")) {
            sender.sendMessage(HEXColor.colorize(plugin.getFpConfig().raw("no-permission")));
            return true;
        }

        plugin.getFpConfig().reload();

        sender.sendMessage(HEXColor.colorize(plugin.getFpConfig().raw("reload-success")));
        return true;
    }
}
