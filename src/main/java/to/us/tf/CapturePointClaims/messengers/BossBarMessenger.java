package to.us.tf.CapturePointClaims.messengers;

import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import to.us.tf.CapturePointClaims.managers.CaptureManager;
import to.us.tf.CapturePointClaims.CapturePoint;
import to.us.tf.CapturePointClaims.CapturePointClaims;
import to.us.tf.CapturePointClaims.Region;
import to.us.tf.CapturePointClaims.managers.RegionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by RoboMWM on 12/23/2016.
 * Bagh wat am i doing
 */
public class BossBarMessenger
{
    CapturePointClaims instance;
    Map<Region, BossBar> cachedRegions = new HashMap<>();
    Map<Player, Region> lastSeenRegion = new HashMap<>();
    CaptureManager captureManager;

    public BossBarMessenger(CapturePointClaims capturePointClaims, CaptureManager captureManager, RegionManager regionManager)
    {
        this.instance = capturePointClaims;
        this.captureManager = captureManager;
        new BukkitRunnable()
        {
            public void run()
            {
                updateBossBar();
            }
        }.runTaskTimer(instance, 200L, 10L);
        new BukkitRunnable()
        {
            public void run()
            {
            for (Player player : instance.getServer().getOnlinePlayers())
            {
                if (!instance.claimWorlds.contains(player.getWorld()))
                {
                    removePlayerFromBossBar(player);
                    continue;
                }

                Region region = regionManager.getRegion(player.getLocation());
                addPlayerToBossBar(player, region);
            }
            }
        }.runTaskTimer(this.instance, 200L, 100L);
    }

    private void updateBossBar()
    {
        for (Region region : cachedRegions.keySet())
        {
            BossBar bar = cachedRegions.get(region);
            CapturePoint capturePoint = captureManager.getCapturePoint(region);

            if (capturePoint == null) //TODO: replace with event listener
            {
                bar.setTitle("Point (" + region.getName() + ") Owned by " + instance.getOwningClanName(region));
                continue;
            }

            if (capturePoint.isEnded()) //Locked point
            {
                bar.setStyle(BarStyle.SOLID);
                bar.setColor(BarColor.BLUE);
                bar.setTitle("Point (" + region.getName() + ") Locked by " + instance.getOwningClanName(region));
                bar.setProgress(capturePoint.getExpirationTimeAsPercentage());
                continue;
            }
            bar.setStyle(BarStyle.SEGMENTED_20);
            bar.setColor(BarColor.RED);
            bar.setProgress(capturePoint.getCaptureProgress());
            String info = "Point (" + region.getName() + ") Owned by " + instance.getOwningClanName(region) + ChatColor.RED + " is under attack!";
            String time = Messenger.formatTimeDifferently(capturePoint.getSecondsToEndGame(), 1);
            bar.setTitle(info + ChatColor.AQUA + " " + time);
        }
    }

    public void addPlayerToBossBar(Player player, Region region)
    {
        Region lastRegion = this.lastSeenRegion.get(player);
        if (lastRegion == region)
            return; //Nothing to do if the player hasn't moved regions

        if (lastRegion != null && cachedRegions.containsKey(lastRegion))
        {
            //Remove player from old bossBar
            cachedRegions.get(lastRegion).removePlayer(player);
        }

        if (!cachedRegions.containsKey(region))
        {
            //Create bossbar and cache it
            String owner = instance.getOwningClanName(region);
            cachedRegions.put(region, instance.getServer().createBossBar("Point (" + region.getName() + ") Owned by " + owner, BarColor.BLUE, BarStyle.SOLID));
        }

        lastSeenRegion.put(player, region);
        cachedRegions.get(region).addPlayer(player);
        player.setCompassTarget(region.getRegionCenter(false));
    }

    public void removePlayerFromBossBar(Player player)
    {
        Region lastRegion = this.lastSeenRegion.remove(player);
        if (lastRegion != null)
            cachedRegions.get(lastRegion).removePlayer(player);
    }
}
