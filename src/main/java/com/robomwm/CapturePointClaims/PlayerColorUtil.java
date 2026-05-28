package com.robomwm.CapturePointClaims;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class PlayerColorUtil {
    private static final NamespacedKey NAME_COLOR_KEY = new NamespacedKey("mountaindewritoes", "name_color");

    public static ChatColor getNameColor(OfflinePlayer offlinePlayer) {
        if (offlinePlayer.isOnline()) {
            Player player = offlinePlayer.getPlayer();
            if (player != null) {
                String color = player.getPersistentDataContainer().get(NAME_COLOR_KEY, PersistentDataType.STRING);
                if (color != null) {
                    try {
                        return ChatColor.valueOf(color);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        // Fallback to hash-based color
        return getDefaultColor(offlinePlayer.getUniqueId());
    }

    public static ChatColor getDefaultColor(UUID uuid) {
        int colorCode = Math.abs(uuid.hashCode());
        String[] acceptableColors = "2,3,4,5,6,9,a,b,c,d,e".split(",");
        colorCode = colorCode % acceptableColors.length;
        return ChatColor.getByChar(acceptableColors[colorCode]);
    }
}
