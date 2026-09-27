package me.telesphoreo.announcer;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

public final class Announcer extends JavaPlugin
{
    private ScheduledTask announcementTask;
    private int nextMessage;

    @Override
    public void onEnable()
    {
        Path yamlPath = getDataFolder().toPath().resolve("config.yml");
        if (!Files.exists(configPath()) && Files.exists(yamlPath))
        {
            try
            {
                convertYamlConfig(yamlPath);
            }
            catch (IOException | InvalidConfigurationException | IllegalArgumentException exception)
            {
                getLogger().severe("Could not convert config.yml to config.toml: " + exception.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }
        if (!Files.exists(configPath()))
        {
            saveResource("config.toml", false);
        }

        final AnnouncementSettings settings;
        try
        {
            settings = loadSettings();
        }
        catch (IOException | IllegalArgumentException exception)
        {
            getLogger().severe("Invalid config.toml: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(Commands.literal("announcer")
                        .requires(source -> source.getSender().hasPermission("announcer.reload"))
                        .then(Commands.literal("reload").executes(context ->
                        {
                            CommandSender sender = context.getSource().getSender();
                            getServer().getGlobalRegionScheduler().run(this, task -> reloadAnnouncements(sender));
                            return Command.SINGLE_SUCCESS;
                        }))
                        .build(), "Reload the Announcer configuration."));

        startAnnouncements(settings);
    }

    private Path configPath()
    {
        return getDataFolder().toPath().resolve("config.toml");
    }

    private void convertYamlConfig(Path yamlPath) throws IOException, InvalidConfigurationException
    {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(yamlPath.toFile());
        final String template;
        try (InputStream stream = getResource("config.toml"))
        {
            template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        Files.writeString(configPath(), ConfigConverter.toToml(yaml, template));
        Files.move(yamlPath, yamlPath.resolveSibling("config.yml.old"));
        getLogger().info("Converted config.yml to config.toml. The old file is config.yml.old.");
    }

    private AnnouncementSettings loadSettings() throws IOException
    {
        TomlParseResult config = Toml.parse(configPath());
        if (config.hasErrors())
        {
            throw new IllegalArgumentException(config.errors().getFirst().toString());
        }
        return AnnouncementSettings.from(config);
    }

    private void reloadAnnouncements(CommandSender sender)
    {
        final AnnouncementSettings settings;
        try
        {
            settings = loadSettings();
        }
        catch (IOException | IllegalArgumentException exception)
        {
            getLogger().warning("Could not reload config.toml: " + exception.getMessage());
            sender.sendRichMessage("<red>Could not reload config.toml. Check the console. Current announcements are unchanged.</red>");
            return;
        }

        startAnnouncements(settings);
        sender.sendRichMessage("<green>Reloaded Announcer."
                + (settings.messages().isEmpty() ? " Announcements are disabled." : "") + "</green>");
    }

    private void startAnnouncements(AnnouncementSettings settings)
    {
        stopAnnouncements();
        if (settings.messages().isEmpty())
        {
            getLogger().info("No messages configured. Announcements are disabled.");
            return;
        }

        nextMessage = 0;
        announcementTask = getServer().getGlobalRegionScheduler().runAtFixedRate(this, task ->
        {
            getServer().broadcast(settings.messages().get(nextMessage));
            nextMessage = (nextMessage + 1) % settings.messages().size();
        }, settings.intervalTicks(), settings.intervalTicks());
    }

    @Override
    public void onDisable()
    {
        stopAnnouncements();
    }

    private void stopAnnouncements()
    {
        if (announcementTask != null)
        {
            announcementTask.cancel();
            announcementTask = null;
        }
    }
}
