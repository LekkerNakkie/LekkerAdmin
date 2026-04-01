package me.lekkernakkie.lekkeradmin.discord.user;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class DiscordRoleService {

    private final LekkerAdmin plugin;
    private final DCBotConfig config;

    public DiscordRoleService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public boolean hasReviewPermission(Member member) {
        if (member == null) {
            return false;
        }

        if (config.getReviewRoleIds() == null || config.getReviewRoleIds().isEmpty()) {
            return true;
        }

        return member.getRoles().stream()
                .anyMatch(role -> config.getReviewRoleIds().contains(role.getId()));
    }

    public void giveApproveRole(Member member) {
        if (member == null || !config.isGiveRoleOnApproveEnabled()) {
            return;
        }

        String roleId = config.getApproveRoleId();
        if (roleId == null || roleId.isBlank()) {
            return;
        }

        Role role = member.getGuild().getRoleById(roleId);
        if (role == null) {
            return;
        }

        if (member.getRoles().stream().anyMatch(existing -> existing.getId().equals(roleId))) {
            return;
        }

        member.getGuild().addRoleToMember(member, role).queue(
                success -> {},
                error -> {}
        );
    }

    public void giveApproveRole(String discordUserId) {
        if (!config.isGiveRoleOnApproveEnabled()) {
            return;
        }

        if (discordUserId == null || discordUserId.isBlank()) {
            return;
        }

        String roleId = config.getApproveRoleId();
        if (roleId == null || roleId.isBlank()) {
            return;
        }

        String guildId = config.getGuildId();
        if (guildId == null || guildId.isBlank()) {
            return;
        }

        if (plugin.getDiscordManager() == null) {
            return;
        }

        JDA jda = plugin.getDiscordManager().getJda();
        if (jda == null) {
            return;
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            return;
        }

        Role role = guild.getRoleById(roleId);
        if (role == null) {
            return;
        }

        guild.retrieveMemberById(discordUserId).queue(member -> {
            if (member.getRoles().stream().anyMatch(existing -> existing.getId().equals(roleId))) {
                return;
            }

            guild.addRoleToMember(member, role).queue(
                    success -> {},
                    error -> {}
            );
        }, error -> {});
    }
}