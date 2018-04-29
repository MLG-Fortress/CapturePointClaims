package com.robomwm.CapturePointClaims.upgrades;

import com.robomwm.CapturePointClaims.CapturePointClaims;
import com.robomwm.CapturePointClaims.events.CaptureStartEvent;
import com.robomwm.CapturePointClaims.point.CapturePoint;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.Iterator;

/**
 * Created on 4/28/2018.
 *
 * @author RoboMWM
 */
public class Turrets implements Listener
{
    private CapturePointClaims plugin;

    public Turrets(CapturePointClaims plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    private void onCaptureStart(CaptureStartEvent event)
    {
        if (event.getPoint().getRegion().getArrows() <= 0)
            return;
        new BukkitRunnable()
        {
            private final int DISTANCE = 32;
            final CapturePoint point = event.getPoint();
            final Location center = point.getRegion().getRegionCenter(true);
            final Location corner = center.clone().add(2, 5, 2);
            final Location otherCorner = center.clone().add(-2, 5, -2);
            Player target;

            @Override
            public void run()
            {
                if (point.isEnded() || !point.getRegion().consumeArrow())
                {
                    cancel();
                    return;
                }

                if (target == null)
                    target = getClosestPlayer(center);
                if (target == null)
                    return;

                Location playerLocation = target.getLocation().add(0, 1.5, 0);

                fireArrow(corner, playerLocation);
                fireArrow(otherCorner, playerLocation);
            }

            private void fireArrow(Location turretLocation, Location playerLocation)
            {
                Vector vector = playerLocation.toVector().subtract(turretLocation.toVector());
                int length = (int)Math.ceil(vector.length());
                if (length == 0 || length > DISTANCE)
                {
                    target = null;
                    return;
                }

                Iterator<Block> blocks = new BlockIterator(corner.getWorld(), corner.toVector(), vector, 0, length);
                blocks.next();
                blocks.next();
                while (blocks.hasNext())
                {
                    Block block = blocks.next();
                    if (block.getType().isSolid())
                    {
                        target = null;
                        return;
                    }
                }
                corner.getWorld().spawnArrow(corner, vector, 2, 0).setGravity(false);
            }

            private Player getClosestPlayer(Location location)
            {
                Player player = null;
                double distance = DISTANCE;
                for (Player onlinePlayer : location.getWorld().getPlayers())
                {
                    double checkDistance = onlinePlayer.getLocation().distanceSquared(location);
                    if (point.getRegion().getRegionManager().isEnemyClan(onlinePlayer, point.getDefender(), false)
                            && checkDistance < distance)
                    {
                        player = onlinePlayer;
                        distance = checkDistance;
                    }
                }
                return player;
            }
        }.runTaskTimer(plugin, 7L, 7L);
    }
}
