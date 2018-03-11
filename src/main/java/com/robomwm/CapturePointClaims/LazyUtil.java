package com.robomwm.CapturePointClaims;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 3/10/2018.
 *
 * @author RoboMWM
 */
public class LazyUtil
{
    public static TextComponent getClickableCommand(String message, String command)
    {
        return getClickableCommand(message, command, command);
    }

    public static TextComponent getClickableCommand(String message, String command, String hover)
    {
        TextComponent textComponent = new TextComponent(message);
        textComponent.setColor(ChatColor.AQUA);
        textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        if (hover != null)
            textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover)));
        return textComponent;
    }

    public static TextComponent getClickableSuggestion(String message, String suggestion, String hover)
    {
        TextComponent textComponent = new TextComponent(message);
        textComponent.setColor(ChatColor.YELLOW);
        textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestion));
        if (hover != null)
            textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover)));
        return textComponent;
    }
}
