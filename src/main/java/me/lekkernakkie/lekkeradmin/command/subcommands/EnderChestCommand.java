package me.lekkernakkie.lekkeradmin.command.subcommands;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.invsee.InvseeService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EnderChestCommand {

    private final LekkerAdmin plugin;
    private final InvseeService invseeService;

    public EnderChestCommand(LekkerAdmin plugin, InvseeService invseeService) {
        this.plugin = plugin;
        this.invseeService = invseeService;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cAlleen spelers kunnen dit command gebruiken.");
            return true;
        }

        if (!plugin.getConfigManager().getMainConfig().isEnderchestEnabled()) {
            player.sendMessage("§cEnderchest bekijken staat uitgeschakeld.");
            return true;
        }

        if (!player.hasPermission("lekkeradmin.enderchest")) {
            player.sendMessage("§cDaar edde gij het lef ni vur..");
            return true;
        }

        if (args.length < 2) {
            invseeService.openOnlineEnderChest(player, player);
            return true;
        }

        String targetName = args[1];
        Player onlineTarget = Bukkit.getPlayerExact(targetName);

        if (onlineTarget != null) {
            invseeService.openOnlineEnderChest(player, onlineTarget);
            return true;
        }

        if (!plugin.getConfigManager().getMainConfig().isEnderchestAllowOffline()) {
            player.sendMessage("§cOffline enderchest bekijken staat uitgeschakeld.");
            return true;
        }

        if (!player.hasPermission("lekkeradmin.enderchest.offline")) {
            player.sendMessage("§cDaar edde gij het lef ni vur..");
            return true;
        }

        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        if (offlineTarget == null || (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline())) {
            player.sendMessage("§cDie speler bestaat niet of heeft nog nooit gejoint.");
            return true;
        }

        invseeService.openOfflineEnderChest(player, offlineTarget);
        return true;
    }
}