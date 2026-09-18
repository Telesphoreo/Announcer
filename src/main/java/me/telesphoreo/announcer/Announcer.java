package me.telesphoreo.announcer;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.io.IOException;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Announcer extends JavaPlugin
{
    private ScheduledTask announcementTask;
    private int nextMessage;

    @Override
    public void onEnable()
    {
        saveDefaultConfig();

        final AnnouncementSettings settings;
        try
        {
            settings = loadSettings();
        }
        catch (IOException | InvalidConfigurationException | IllegalArgumentException exception)
        {
            getLogger().severe("Invalid config.yml: " + exception.getMessage());
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

    private AnnouncementSettings loadSettings() throws IOException, InvalidConfigurationException
    {
        YamlConfiguration config = new YamlConfiguration();
        config.load(new File(getDataFolder(), "config.yml"));
        return AnnouncementSettings.from(config);
    }

    private void reloadAnnouncements(CommandSender sender)
    {
        final AnnouncementSettings settings;
        try
        {
            settings = loadSettings();
        }
        catch (IOException | InvalidConfigurationException | IllegalArgumentException exception)
        {
            getLogger().warning("Could not reload config.yml: " + exception.getMessage());
            sender.sendRichMessage("<red>Could not reload config.yml. Check the console. Current announcements are unchanged.</red>");
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
