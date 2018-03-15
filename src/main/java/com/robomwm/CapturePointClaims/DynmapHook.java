package com.robomwm.CapturePointClaims;

import com.robomwm.CapturePointClaims.events.CaptureFinishedEvent;
import com.robomwm.CapturePointClaims.region.Region;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.DynmapAPI;
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
        if (dynmapAPI == null)
            return;
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
        MarkerSet markerSet = dynmapAPI.getMarkerAPI().getMarkerSet("claimposts");
        if (markerSet == null)
            markerSet = dynmapAPI.getMarkerAPI().createMarkerSet("claimposts", "claimposts", null, false);
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
            x[1] = topLeftCorner.getBlockX(); z[1] = bottomRightCorner.getBlockZ(); //bottom left corner
            x[1] = bottomRightCorner.getBlockX(); z[1] = topLeftCorner.getBlockZ(); //top right corner
            x[1] = bottomRightCorner.getBlockX(); z[1] = bottomRightCorner.getBlockZ(); //bottom right corner

            markerSet.createAreaMarker(region.getName(), region.getName() + " owned by " + region.getOwner().getName(), false, center.getWorld().getName(), x, z, false);
        }
    }
}
