# Announcer

Send repeating chat announcements on Paper or Folia 26.2. Use Java 25.

## Install

1. Run `./gradlew build` (`.\gradlew.bat build` on Windows).
2. Copy `build/libs/Announcer-1.0.jar` into your server's `plugins` directory.
3. Start the server to create `plugins/Announcer/config.toml`.
4. Edit the configuration.
5. Run `/announcer reload` to apply changes.

If `plugins/Announcer/config.yml` exists and `config.toml` does not, Announcer converts it to `config.toml` at startup.
It keeps the old file as `config.yml.old`. If the conversion fails, Announcer logs the error, keeps `config.yml`, and disables itself.

```toml
interval-seconds = 300
prefix = "<gold>[Announcer]</gold> "
messages = [
    "<green>Welcome to the server!</green>",
    "<yellow>Please be kind to other players.</yellow>",
]
```

Put every string in double quotes. Colons and apostrophes are safe inside the quotes.
Write `\"` for a double quote and `\\` for a backslash. For example, write the MiniMessage escape `\<` as `\\<`.
Set `prefix = ""` to omit the prefix. Include any space you want after the prefix.
Format the prefix and each message separately with MiniMessage.
Do not use legacy `&` or section-sign color codes. Announcer does not convert them.

Announcer sends one message per interval in list order, then repeats the list.
It waits one interval before the first message. Set `messages = []` to disable announcements.
Use a positive whole number for `interval-seconds`. Timing uses 20 server ticks per second.
Server lag or pauses can delay announcements.

Announcer uses the global region scheduler on Paper and Folia.
It parses messages at startup and on reload, and cancels its task on shutdown.
It has no plugin dependencies. On first start, Paper downloads the tomlj library from Maven Central.

## Reload

Run `/announcer reload` in game or `announcer reload` in the server console.
You need `announcer.reload`. Operators have this permission by default.

After a successful reload, Announcer starts at the first message and waits one new interval.
If the file cannot be read or the configuration is invalid, it keeps the current announcements.
Check the console for the error, correct the file, and run `/announcer reload` again.
