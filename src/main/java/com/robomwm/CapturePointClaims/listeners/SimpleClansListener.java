package com.robomwm.CapturePointClaims.listeners;

import com.robomwm.CapturePointClaims.CapturePointClaims;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Created on 3/9/2018.
 *
 * @author RoboMWM
 */
public class SimpleClansListener implements Listener
{
    private CapturePointClaims instance;
    public SimpleClansListener(CapturePointClaims plugin)
    {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        instance = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void processor(PlayerCommandPreprocessEvent event)
    {
        String message = event.getMessage();
        //Aliases
        switch(message)
        {
            case "/home":
                message = "/clan home";
                break;
            case "/sethome":
                message = "/clan sethome";
                break;
        }

        String[] heyo = message.split(" ");
        String command = heyo[0].substring(1).toLowerCase();
        String[] args = new String[heyo.length - 1];
        for (int i = 1; i < heyo.length; i++)
            args[i - 1] = heyo[i].toLowerCase();

        Player player = event.getPlayer();

        switch (command)
        {
            case "clan":
                event.setCancelled(clanHandler(player, command, args));
                break;
        }
    }

    private boolean clanHandler(Player player, String command, String[] args)
    {
        if (args.length < 1)
            return false;

        Clan clan = instance.getClanManager().getClanByPlayerUniqueId(player.getUniqueId());
        if (clan == null)
            return false;

        switch (args[0])
        {
            case "home":
                //player.sendMessage("Use /tppoint");
                if (clan.getHomeLocation() == null)
                    return false;
                if (instance.getRegionManager().getRegion(clan.getHomeLocation()) == null
                        || instance.getRegionManager().isEnemyClaim(clan.getHomeLocation(), player, true))
                {
                    player.sendMessage(ChatColor.RED + "We can't seem to get a lock on your clan's old /home! Perhaps try setting a new one in a claimed area?");
                    return true;
                }
                return false;
            case "sethome":
                if (instance.getRegionManager().getRegion(player.getLocation()) == null)
                {
                    player.sendMessage(ChatColor.GRAY + "*All you hear is static as you vainly attempt to register this location as your clan's new home...*");
                    return true;
                }
                if (instance.getRegionManager().isEnemyClaim(player.getLocation(), player, true))
                {
                    player.sendMessage(ChatColor.RED + "We can't register this location as your clan's new home until you capture its post!");
                    return true;
                }
        }
        return false;
    }
}
