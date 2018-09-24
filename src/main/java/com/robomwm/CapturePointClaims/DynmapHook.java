package com.robomwm.CapturePointClaims;

import com.robomwm.CapturePointClaims.events.CaptureFinishedEvent;
import com.robomwm.CapturePointClaims.region.Region;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;

/**
 * Created on 3/11/2018.
 * My packages are a mess in this project
 * @author RoboMWM
 */
public class DynmapHook implements Listener
{
    private CapturePointClaims plugin;
    private DynmapAPI dynmapAPI;
    public DynmapHook(CapturePointClaims plugin)
    {
        this.plugin = plugin;

        dynmapAPI = (DynmapAPI)plugin.getServer().getPluginManager().getPlugin("dynmap");
        if (dynmapAPI == null || !plugin.getServer().getPluginManager().isPluginEnabled("dynmap"))
        {
            plugin.getLogger().info("Dynmap not installed or enabled.");
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                doStuff();
            }
        }.runTask(plugin);
    }

    @EventHandler
    private void updateOnCapture(CaptureFinishedEvent event)
    {
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                doStuff();
            }
        }.runTask(plugin);
    }

    //Does the dynmap stuff ahahahaaa yea idk what to name it so I used generic "stuff" for the first time in my life xd
    private void doStuff()
    {
        MarkerSet markerSet = dynmapAPI.getMarkerAPI().getMarkerSet("Claim Posts");
        if (markerSet == null)
            markerSet = dynmapAPI.getMarkerAPI().createMarkerSet("Claim Posts", "Claim Posts", null, false);
        markerSet.getAreaMarkers().clear();
        for (Region region : plugin.getRegionManager().getRegions())
        {
            if (region.getOwner() == null)
                continue;
            Location center = region.getRegionCenter(false);
            Location topLeftCorner = new Location(center.getWorld(), center.getX() - 200, 0,center.getZ() - 200);
            Location bottomRightCorner = new Location(center.getWorld(), center.getX() + 200, 0,center.getZ() + 200);

            //I have no idea about this part of the API, javadocs are quite sparse about it.
            //I thought I had to specify every single number in the range according to javadoc... so yea, I'll look at
            //some existing code instead...
            //Maybe they should use the language "corners."
            double[] x = new double[4];
            double[] z = new double[4];

            x[0] = topLeftCorner.getBlockX(); z[0] = topLeftCorner.getBlockZ(); //Top left corner
            x[1] = bottomRightCorner.getBlockX(); z[1] = topLeftCorner.getBlockZ(); //top right corner
            x[2] = bottomRightCorner.getBlockX(); z[2] = bottomRightCorner.getBlockZ(); //bottom right corner
            x[3] = topLeftCorner.getBlockX(); z[3] = bottomRightCorner.getBlockZ(); //bottom left corner

            Clan clan = plugin.getClanManager().getClanByPlayerUniqueId(region.getOwner().getUniqueId());
            AreaMarker marker;
            if (clan != null)
                marker = markerSet.createAreaMarker(region.getName(), region.getName() + " owned by " +
                    clan.getTag().toUpperCase() + " " + region.getOwner().getName(), false, center.getWorld().getName(), x, z, false);
            else
                marker = markerSet.createAreaMarker(region.getName(), region.getName() + " owned by " +
                    region.getOwner().getName(), false, center.getWorld().getName(), x, z, false);
            int color = colorConverter(getColorClanOrPlayer(region.getOwner()));
            marker.setFillStyle(0.2D, color);
            marker.setLineStyle(2, 0.9D, color);
        }
    }

    private ChatColor getColorClanOrPlayer(OfflinePlayer player)
    {
        Clan clan = plugin.getClanManager().getClanByPlayerUniqueId(player.getUniqueId());
        if (clan == null)
            return plugin.getGrandAPI().getGrandPlayerManager().getGrandPlayer(player).getNameColor();
        return ChatColor.getByChar(clan.getColorTag().substring(1));
    }

    //hex codes as listed here: http://www.planetminecraft.com/blog/minecraft-color-codes-2906205/Feel
    private int colorConverter(ChatColor color)
    {
        if (color == null)
            color = ChatColor.WHITE;
        switch (color)
        {
            case BLACK:
                return 0;
            case DARK_BLUE:
                return 170;
            case DARK_GREEN:
                return 43520;
            case DARK_AQUA:
                return 43690;
            case DARK_RED:
                return 11141120;
            case DARK_PURPLE:
                return 11141290;
            case GOLD:
                return 16755200;
            case GRAY:
                return 11184810;
            case DARK_GRAY:
                return 5592405;
            case BLUE:
                return 5592575;
            case GREEN:
                return 5635925;
            case AQUA:
                return 5636095;
            case RED:
                return 16733525;
            case LIGHT_PURPLE:
                return 16733695;
            case YELLOW:
                return 16777045;
            case WHITE:
                return 166777215;
            default:
                return 0;
        }
    }
}
