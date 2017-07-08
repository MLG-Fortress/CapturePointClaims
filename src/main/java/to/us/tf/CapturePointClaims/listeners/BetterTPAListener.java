package to.us.tf.CapturePointClaims.listeners;

import me.robomwm.BetterTPA.BetterTPA;
import me.robomwm.BetterTPA.PreTPATeleportEvent;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import to.us.tf.CapturePointClaims.CapturePointClaims;

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
        if (instance.isEnemyClaim(event.getTargetLocation(), event.getPlayer(), false))
        {
            event.setCancelled(true);
            event.setReason("You cannot teleport to an enemy-clan's claim.");
        }
        else if (instance.isEnemyClaim(event.getPlayer().getLocation(), event.getPlayer(), false))
        {
            event.setWarmup(400L);
        }
    }
}
