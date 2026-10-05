package me.telesphoreo.announcer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

final class AnnouncementFormatter
{
    private static final Pattern URL = Pattern.compile("(?i)https?://[^\\s<>\"']+");

    static Component parse(String message)
    {
        return linkify(MiniMessage.miniMessage().deserialize(message), false);
    }

    private static Component linkify(Component component, boolean hasClick)
    {
        hasClick = hasClick || component.clickEvent() != null;
        List<Component> children = new ArrayList<>();
        Component result = component.children(List.of());
        if (!hasClick && component instanceof TextComponent text)
        {
            var matcher = URL.matcher(text.content());
            int position = 0;
            while (matcher.find())
            {
                String url = trimUrl(matcher.group());
                children.add(Component.text(text.content().substring(position, matcher.start())));
                children.add(Component.text(url).clickEvent(ClickEvent.openUrl(url)));
                position = matcher.start() + url.length();
            }
            if (position > 0)
            {
                children.add(Component.text(text.content().substring(position)));
                result = ((TextComponent) result).content("");
            }
        }
        for (Component child : component.children())
        {
            children.add(linkify(child, hasClick));
        }
        return result.children(children);
    }

    private static String trimUrl(String url)
    {
        int end = url.length();
        while (end > 0)
        {
            char last = url.charAt(end - 1);
            if (".,;:!?".indexOf(last) >= 0
                    || last == ')' && unbalanced(url.substring(0, end), '(', ')')
                    || last == ']' && unbalanced(url.substring(0, end), '[', ']')
                    || last == '}' && unbalanced(url.substring(0, end), '{', '}'))
            {
                end--;
            }
            else
            {
                break;
            }
        }
        return url.substring(0, end);
    }

    private static boolean unbalanced(String url, char open, char close)
    {
        return url.chars().filter(character -> character == close).count()
                > url.chars().filter(character -> character == open).count();
    }
}
