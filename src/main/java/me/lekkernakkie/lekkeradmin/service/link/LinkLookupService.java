package me.lekkernakkie.lekkeradmin.service.link;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;

import java.util.Optional;

public class LinkLookupService {

    private final LinkService linkService;

    public LinkLookupService(LekkerAdmin plugin) {
        this.linkService = new LinkService(plugin);
    }

    public Optional<DiscordMinecraftLink> findByDiscordUserId(String discordUserId) {
        return linkService.findByDiscordUserId(discordUserId);
    }

    public Optional<DiscordMinecraftLink> findByMinecraftName(String minecraftName) {
        return linkService.findByMinecraftName(minecraftName);
    }
}