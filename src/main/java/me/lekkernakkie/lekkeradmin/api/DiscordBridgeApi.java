package me.lekkernakkie.lekkeradmin.api;

import java.util.function.Consumer;

public interface DiscordBridgeApi {

    boolean isReady();

    void sendToChannel(String channelId, String message);

    void registerMessageListener(Consumer<DiscordChatMessage> consumer);

    void unregisterMessageListener(Consumer<DiscordChatMessage> consumer);
}