package me.lekkernakkie.lekkeradmin.discord.bridge;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.api.DiscordBridgeApi;
import me.lekkernakkie.lekkeradmin.api.DiscordChatMessage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class DiscordBridgeService extends ListenerAdapter implements DiscordBridgeApi {

    private final LekkerAdmin plugin;
    private final JDA jda;
    private final List<Consumer<DiscordChatMessage>> listeners = new CopyOnWriteArrayList<>();

    public DiscordBridgeService(LekkerAdmin plugin, JDA jda) {
        this.plugin = plugin;
        this.jda = jda;
    }

    public void registerService() {
        Bukkit.getServicesManager().register(
                DiscordBridgeApi.class,
                this,
                plugin,
                ServicePriority.Normal
        );

        jda.addEventListener(this);
        plugin.debug("DiscordBridgeApi registered.");
    }

    public void shutdown() {
        try {
            Bukkit.getServicesManager().unregister(DiscordBridgeApi.class, this);
        } catch (Exception ignored) {
        }

        try {
            jda.removeEventListener(this);
        } catch (Exception ignored) {
        }

        listeners.clear();
        plugin.debug("DiscordBridgeApi unregistered.");
    }

    @Override
    public boolean isReady() {
        return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }

    @Override
    public void sendToChannel(String channelId, String message) {
        if (!isReady()) {
            return;
        }

        if (channelId == null || channelId.isBlank() || message == null || message.isBlank()) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Discord bridge could not find channel with id: " + channelId);
            return;
        }

        channel.sendMessage(message).queue(
                success -> {},
                error -> plugin.getLogger().warning("Failed to send Discord bridge message: " + error.getMessage())
        );
    }

    @Override
    public void registerMessageListener(Consumer<DiscordChatMessage> consumer) {
        if (consumer != null) {
            listeners.add(consumer);
        }
    }

    @Override
    public void unregisterMessageListener(Consumer<DiscordChatMessage> consumer) {
        if (consumer != null) {
            listeners.remove(consumer);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) {
            return;
        }

        String content = event.getMessage().getContentDisplay();

        if ((content == null || content.isBlank()) && !event.getMessage().getAttachments().isEmpty()) {
            content = "[attachment x" + event.getMessage().getAttachments().size() + "]";
        }

        if (content == null || content.isBlank()) {
            return;
        }

        String authorName = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getAuthor().getName();

        DiscordChatMessage message = new DiscordChatMessage(
                event.getGuild().getId(),
                event.getChannel().getId(),
                event.getAuthor().getId(),
                authorName,
                content,
                event.getAuthor().isBot(),
                event.isWebhookMessage()
        );

        for (Consumer<DiscordChatMessage> listener : listeners) {
            try {
                listener.accept(message);
            } catch (Exception ex) {
                plugin.getLogger().warning("Discord bridge listener failed: " + ex.getMessage());
            }
        }
    }
}