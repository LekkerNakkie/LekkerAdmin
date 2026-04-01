package me.lekkernakkie.lekkeradmin.discord.log;

import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.List;

public class MinecraftLogMessage {

    private final String content;
    private final MessageEmbed embed;
    private final List<String> extraLines;

    public MinecraftLogMessage(String content, MessageEmbed embed, List<String> extraLines) {
        this.content = content == null ? "" : content;
        this.embed = embed;
        this.extraLines = extraLines == null ? new ArrayList<>() : extraLines;
    }

    public String getContent() {
        return content;
    }

    public MessageEmbed getEmbed() {
        return embed;
    }

    public List<String> getExtraLines() {
        return extraLines;
    }

    public boolean hasEmbed() {
        return embed != null;
    }

    public boolean hasContent() {
        return content != null && !content.isBlank();
    }

    public boolean hasExtraLines() {
        return extraLines != null && !extraLines.isEmpty();
    }
}