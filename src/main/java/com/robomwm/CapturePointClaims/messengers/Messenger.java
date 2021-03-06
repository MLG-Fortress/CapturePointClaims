package com.robomwm.CapturePointClaims.messengers;

import com.robomwm.CapturePointClaims.CapturePointClaims;
import com.robomwm.CapturePointClaims.LazyUtil;
import com.robomwm.CapturePointClaims.region.Region;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import javax.annotation.Nullable;

/**
 * Created by RoboMWM on 12/23/2016.
 * Don't shoot the messenger!
 */
public class Messenger {
    /**
     * Send a chat message to all members of a certain clan
     *
     * @param clan    Clan to send message to
     * @param message the message
     */
    private static void chatMessageToClan(Clan clan, String message, String command)
    {
        message = clan.getColorTag() + ": " + message;
        for (ClanPlayer clanPlayer : clan.getOnlineMembers()) {
            clanPlayer.toPlayer().sendMessage(LazyUtil.getClickableCommand(message, command));
        }
    }

    private static String cleanCoords(Location location)
    {
        return "x: " + String.valueOf(location.getBlockX()) + ", z: " + String.valueOf(location.getBlockZ());
    }

    /**
     * Thanks Lax
     * @param seconds
     * @return
     */
    public static String formatTime(Long seconds)
    {
        return formatTime(seconds, 1);
    }
    private static String formatTime(Long seconds, int depth)
    {
        if (seconds == null || seconds < 1) {
            return "moments";
        }

        if (seconds < 60) {
            return seconds + " seconds";
        }

        if (seconds < 3600) {
            Long count = (long) Math.ceil(seconds / 60);
            String res;
            if (count > 1) {
                res = count + " minutes";
            } else {
                res = "1 minute";
            }
            Long remaining = seconds % 60;
            if (depth > 0 && remaining >= 5) {
                return res + ", " + formatTime(remaining, --depth);
            }
            return res;
        }
        if (seconds < 86400) {
            Long count = (long) Math.ceil(seconds / 3600);
            String res;
            if (count > 1) {
                res = count + " hours";
            } else {
                res = "1 hour";
            }
            if (depth > 0) {
                return res + ", " + formatTime(seconds % 3600, --depth);
            }
            return res;
        }
        Long count = (long) Math.ceil(seconds / 86400);
        String res;
        if (count > 1) {
            res = count + " days";
        } else {
            res = "1 day";
        }
        if (depth > 0) {
            return res + ", " + formatTime(seconds % 86400, --depth);
        }
        return res;
    }

    public static String formatTimeDifferently(int seconds, int depth) //kek
    {
        if (seconds < 0) {
            return "";
        }

        if (seconds < 60) {
            return ":" + String.format("%02d", seconds); //http://stackoverflow.com/questions/275711/add-leading-zeroes-to-number-in-java
        }

        Long count = (long) Math.ceil(seconds / 60);
        String res = count.toString();

        int remaining = seconds % 60;
        if (depth > 0 && remaining >= 0) {
            return res + formatTimeDifferently(remaining, --depth);
        }
        return res + ":00";
    }

    /**
     * Alerts members of defending clans of an attack.
     * @param defendingClan
     */
    public static void alertMembersOfAttack(@Nullable Clan defendingClan, Region region)
    {
        if (defendingClan != null)
        {
            chatMessageToClan(defendingClan, "Help defend! /tppost " + region.getName(), "/tppost " + region.getName());
        }
        else if (region.getOwner() != null)
        {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "umail text " +
                    region.getOwner().getName() + " Post " + region.getName() + " is under attack!");
        }

        //chatMessageToClan(attackingClan, "We are attacking capture point " + region.getName() + " which is owned by " + defendingClanTag);
    }


    public static void mailPlayerOrClan(CapturePointClaims plugin, OfflinePlayer player, String message)
    {
        if (player == null)
            return;
        Clan clan = plugin.getClanManager().getClanByPlayerUniqueId(player.getUniqueId());
        if (clan == null)
        {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "umail text " + player.getName() + " " + message);
            return;
        }

        clan.addBb(clan.getColorTag(), message, false);
        for (String allyString : clan.getAllies())
            plugin.getClanManager().getClan(allyString).addBb("", clan.getColorTag() + ChatColor.RESET + " " + message, false);
    }
}
