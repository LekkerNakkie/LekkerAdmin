package me.lekkernakkie.lekkeradmin.command.subcommands;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.invsee.InvseeService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InvseeCommand {

    private final LekkerAdmin plugin;
    private final InvseeService invseeService;

    public InvseeCommand(LekkerAdmin plugin, InvseeService invseeService) {
        this.plugin = plugin;
        this.invseeService = invseeService;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cAlleen spelers kunnen dit command gebruiken.");
            return true;
        }

        if (!plugin.getConfigManager().getMainConfig().isInvseeEnabled()) {
            player.sendMessage("§cInvsee staat uitgeschakeld.");
            return true;
        }

        if (!player.hasPermission("lekkeradmin.invsee")) {
            player.sendMessage("§cDaar edde gij het lef ni vur..");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§eGebruik: §7/lekkeradmin invsee <speler>");
            return true;
        }

        String targetName = args[1];
        Player onlineTarget = Bukkit.getPlayerExact(targetName);

        if (onlineTarget != null) {
            invseeService.openOnlineInventory(player, onlineTarget);
            return true;
        }

        if (!plugin.getConfigManager().getMainConfig().isInvseeAllowOffline()) {
            player.sendMessage("§cOffline invsee staat uitgeschakeld.");
            return true;
        }

        if (!player.hasPermission("lekkeradmin.invsee.offline")) {
            player.sendMessage("§cDaar edde gij het lef ni vur..");
            return true;
        }

        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        if (offlineTarget == null || (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline())) {
            player.sendMessage("§cDie speler bestaat niet of heeft nog nooit gejoint.");
            return true;
        }

        invseeService.openOfflinePlaceholder(player, offlineTarget);
        return true;
    }
}