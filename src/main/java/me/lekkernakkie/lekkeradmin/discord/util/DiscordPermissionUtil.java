package me.lekkernakkie.lekkeradmin.discord.util;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.discord.user.DiscordRoleService;
import net.dv8tion.jda.api.entities.Member;

public class DiscordPermissionUtil {

    private final DiscordRoleService discordRoleService;

    public DiscordPermissionUtil(LekkerAdmin plugin) {
        this.discordRoleService = new DiscordRoleService(plugin);
    }

    public boolean canReviewApplications(Member member) {
        return discordRoleService.hasReviewPermission(member);
    }
}