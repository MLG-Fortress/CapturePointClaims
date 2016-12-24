package to.us.tf.CapturePointClaims;

import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by RoboMWM on 12/23/2016.
 * Bagh wat am i doing
 */
public class BossBarMessenger
{
    CapturePointClaims instance;
    Map<RegionCoordinates, CapturePoint> regionsUnderAttack = new HashMap<>();
    Map<RegionCoordinates, BossBar> cachedRegions = new HashMap<>();
    Map<Player, RegionCoordinates> lastSeenRegion = new HashMap<>();

    public BossBarMessenger(CapturePointClaims capturePointClaims, RegionCoordinates regionCoordinates)
    {
        this.instance = capturePointClaims;
        new BukkitRunnable()
        {
            public void run()
            {
                updateBossBar();

                for (Player player : instance.getServer().getOnlinePlayers())
                {
                    if (!instance.claimWorlds.contains(player.getWorld()))
                    {
                        removePlayerFromBossBar(player);
                        continue;
                    }

                    RegionCoordinates region = regionCoordinates.fromLocation(player.getLocation());
                    addPlayerToBossBar(player, region);
                }
            }
        }.runTaskTimer(this.instance, 200L, 20L);
    }

    public void addRegionUnderAttack(RegionCoordinates regionCoordinates, CapturePoint capturePoint)
    {

    }

    private void updateBossBar()
    {
        for (RegionCoordinates region : cachedRegions.keySet())
        {
            if (!regionsUnderAttack.containsKey(region))
                continue;

            CapturePoint capturePoint = regionsUnderAttack.get(region);
            BossBar bar = cachedRegions.get(region);
            bar.setStyle(BarStyle.SEGMENTED_20);
            bar.setColor(BarColor.RED);
            bar.setProgress(capturePoint.getCaptureProgress() / 100);
            String info = capturePoint.getAttackingClan().getColorTag() + ChatColor.RESET + " attacking " + capturePoint.getOwningClanTag() + ChatColor.RESET;
            String time = Messenger.formatTimeDifferently(capturePoint.getSecondsToEndGame(), 1);
            bar.setTitle(info + " " + time);
        }
    }

    public void addPlayerToBossBar(Player player, RegionCoordinates region)
    {
        RegionCoordinates lastRegion = this.lastSeenRegion.get(player);
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
            String owner = region.getOwningClanTag();
            if (owner == null) owner = "Wilderness";
            cachedRegions.put(region, instance.getServer().createBossBar("Owned by " + owner, BarColor.BLUE, BarStyle.SOLID));
        }

        cachedRegions.get(region).addPlayer(player);
    }

    public void removePlayerFromBossBar(Player player)
    {
        RegionCoordinates lastRegion = this.lastSeenRegion.remove(player);
        if (lastRegion != null)
            cachedRegions.get(lastRegion).removePlayer(player);
    }


}
