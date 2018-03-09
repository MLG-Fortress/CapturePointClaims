package com.robomwm.CapturePointClaims.listeners;

import com.robomwm.CapturePointClaims.CapturePointClaims;
import me.robomwm.BetterTPA.BetterTPA;
import me.robomwm.BetterTPA.PreTPATeleportEvent;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Created on 3/18/2017.
 *
 * @author RoboMWM
 */
public class BetterTPAListener implements Listener
{
    BetterTPA betterTPA;
    CapturePointClaims instance;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPreTeleport(PreTPATeleportEvent event)
    {
        if (instance.getRegionManager().isEnemyClaim(event.getTargetLocation(), event.getPlayer(), false))
        {
            event.setCancelled(true);
            event.setReason("Your destination is within an enemy clan's claim.");
        }
        else if (instance.getRegionManager().isEnemyClaim(event.getPlayer().getLocation(), event.getPlayer(), false))
        {
            event.setWarmup(400L);
            event.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Interference from the enemy clan's capture point is causing the lock-on process to take longer. Please be patient and hold still if you wish to be teleported...");
        }
    }
}
