package me.lekkernakkie.lekkeradmin.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.vanish.VanishService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VanishCommand implements CommandExecutor {

    private final LekkerAdmin plugin;
    private final VanishService vanishService;

    public VanishCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.vanishService = plugin.getVanishService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.lang().message(
                    "vanish.player-only",
                    "&cDit commando kan enkel door spelers gebruikt worden."
            ));
            return true;
        }

        if (!plugin.getConfigManager().getMainConfig().isVanishEnabled()) {
            player.sendMessage(plugin.lang().message(
                    "vanish.disabled-feature",
                    "&cVanish staat uitgeschakeld."
            ));
            return true;
        }

        if (!player.hasPermission("lekkeradmin.vanish") && !player.hasPermission("lekkeradmin.admin")) {
            player.sendMessage(plugin.lang().message(
                    "vanish.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        if (vanishService.isVanished(player.getUniqueId())) {
            vanishService.disableVanish(player, true, "manual");
        } else {
            vanishService.enableVanish(player, true, "manual");
        }

        return true;
    }
}