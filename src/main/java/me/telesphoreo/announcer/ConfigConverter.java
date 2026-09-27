package me.telesphoreo.announcer;

import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.tomlj.Toml;

final class ConfigConverter
{
    private ConfigConverter()
    {
    }

    static String toToml(ConfigurationSection yaml, String template)
    {
        if (!yaml.isInt("interval-seconds") && !yaml.isLong("interval-seconds"))
        {
            throw new IllegalArgumentException("Set interval-seconds to a positive whole number.");
        }
        if (yaml.contains("prefix") && !yaml.isString("prefix"))
        {
            throw new IllegalArgumentException("Set prefix to a MiniMessage string or an empty string.");
        }
        List<?> messages = yaml.getList("messages");
        if (messages == null)
        {
            throw new IllegalArgumentException("Set messages to a list of MiniMessage strings, or [] to disable announcements.");
        }

        StringBuilder messagesToml = new StringBuilder("messages = [\n");
        for (int index = 0; index < messages.size(); index++)
        {
            if (!(messages.get(index) instanceof String message))
            {
                throw new IllegalArgumentException("Set message " + (index + 1) + " to a MiniMessage string.");
            }
            messagesToml.append("    ").append(quote(message)).append(",\n");
        }
        messagesToml.append("]\n");

        String toml = template.replace("\r\n", "\n")
                .replace("interval-seconds = 300", "interval-seconds = " + yaml.getLong("interval-seconds"))
                .replace("prefix = \"\"", "prefix = " + quote(yaml.getString("prefix", "")));
        return toml.substring(0, toml.indexOf("messages = [")) + messagesToml;
    }

    private static String quote(String value)
    {
        return "\"" + Toml.tomlEscape(value) + "\"";
    }
}
