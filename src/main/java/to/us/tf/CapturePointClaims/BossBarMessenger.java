package to.us.tf.CapturePointClaims;

import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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

    public BossBarMessenger(CapturePointClaims capturePointClaims, CaptureManager captureManager)
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
            RegionManager regionManager = new RegionManager();
            public void run()
            {
            for (Player player : instance.getServer().getOnlinePlayers())
            {
                if (!instance.claimWorlds.contains(player.getWorld()))
                {
                    removePlayerFromBossBar(player);
                    continue;
                }

                Region region = regionManager.fromLocation(player.getLocation());
                addPlayerToBossBar(player, region);
            }
            }
        }.runTaskTimer(this.instance, 200L, 100L);
    }

    private void updateBossBar()
    {
        for (Region region : cachedRegions.keySet())
        {
            if (!captureManager.pointsBeingCaptured.containsKey(region))
                continue;

            CapturePoint capturePoint = captureManager.pointsBeingCaptured.get(region);
            BossBar bar = cachedRegions.get(region);
            if (capturePoint.isEnded())
            {
                bar.setStyle(BarStyle.SOLID);
                bar.setColor(BarColor.BLUE);
                bar.setTitle("Locked by " + instance.getOwningClanString(region));
                bar.setProgress(capturePoint.getExpirationTimeAsPercentage());
                continue;
            }
            bar.setStyle(BarStyle.SEGMENTED_20);
            bar.setColor(BarColor.RED);
            bar.setProgress(capturePoint.getCaptureProgress() / 100D);
            String info = capturePoint.getAttackingClan().getColorTag() + ChatColor.RESET + " attacking " + capturePoint.getOwningClanColorTag() + ChatColor.RESET;
            String time = Messenger.formatTimeDifferently(capturePoint.getSecondsToEndGame(), 1);
            bar.setTitle(info + " " + time);
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
            String owner = instance.getOwningClanString(region);
            cachedRegions.put(region, instance.getServer().createBossBar("Owned by " + owner, BarColor.BLUE, BarStyle.SOLID));
        }

        lastSeenRegion.put(player, region);
        cachedRegions.get(region).addPlayer(player);
    }

    public void removePlayerFromBossBar(Player player)
    {
        Region lastRegion = this.lastSeenRegion.remove(player);
        if (lastRegion != null)
            cachedRegions.get(lastRegion).removePlayer(player);
    }


}
