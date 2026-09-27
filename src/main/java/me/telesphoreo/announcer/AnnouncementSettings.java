package me.telesphoreo.announcer;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.tomlj.TomlArray;
import org.tomlj.TomlTable;

record AnnouncementSettings(long intervalTicks, List<Component> messages)
{
    static AnnouncementSettings from(TomlTable config)
    {
        if (!config.isLong("interval-seconds"))
        {
            throw new IllegalArgumentException("Set interval-seconds to a positive whole number.");
        }
        long seconds = config.getLong("interval-seconds");
        if (seconds < 1 || seconds > Long.MAX_VALUE / 20)
        {
            throw new IllegalArgumentException("Set interval-seconds between 1 and " + Long.MAX_VALUE / 20 + ".");
        }

        if (config.contains("prefix") && !config.isString("prefix"))
        {
            throw new IllegalArgumentException("Set prefix to a MiniMessage string or an empty string.");
        }
        if (!config.isArray("messages"))
        {
            throw new IllegalArgumentException("Set messages to a list of MiniMessage strings, or [] to disable announcements.");
        }
        TomlArray configuredMessages = config.getArrayOrEmpty("messages");

        MiniMessage miniMessage = MiniMessage.miniMessage();
        Component prefix = miniMessage.deserialize(config.getString("prefix", () -> ""));
        List<Component> messages = new ArrayList<>();
        for (int index = 0; index < configuredMessages.size(); index++)
        {
            if (!(configuredMessages.get(index) instanceof String message))
            {
                throw new IllegalArgumentException("Set message " + (index + 1) + " to a MiniMessage string.");
            }
            messages.add(Component.empty().append(prefix).append(miniMessage.deserialize(message)));
        }
        return new AnnouncementSettings(seconds * 20, List.copyOf(messages));
    }
}
