package com.robomwm.CapturePointClaims.messengers;

import com.robomwm.CapturePointClaims.point.CapturePoint;
import com.robomwm.CapturePointClaims.CapturePointClaims;
import com.robomwm.CapturePointClaims.region.Region;
import com.robomwm.grandioseapi.player.GrandPlayer;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import com.robomwm.CapturePointClaims.point.CaptureManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by RoboMWM on 12/23/2016.
 * Bagh wat am i doing
 */
public class BossBarMessenger implements Listener
{
    private CapturePointClaims instance;
    private Map<Region, BossBar> bossBars = new HashMap<>();
    private Map<Player, Region> lastSeenRegion = new HashMap<>();
    private Set<Player> playersMoved = new HashSet<>();
    private CaptureManager captureManager;

    public BossBarMessenger(CapturePointClaims capturePointClaims, CaptureManager captureManager)
    {
        capturePointClaims.getServer().getPluginManager().registerEvents(this, capturePointClaims);
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
            @Override
            public void run()
            {
                for (Player player : playersMoved)
                {
                    updateViewedBossBar(player);
                }
                playersMoved.clear();
            }
        }.runTaskTimer(capturePointClaims, 100L, 20L);
    }

    @EventHandler
    private void onWorldChange(PlayerChangedWorldEvent event)
    {
        updateViewedBossBar(event.getPlayer());
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event)
    {
        if (event.getFrom().getWorld() != event.getTo().getWorld())
            return; //handled in PlayerChangedWorldEvent
        if (event.getFrom().distanceSquared(event.getTo()) == 0)
            return;

        playersMoved.add(event.getPlayer());
    }

    private void updateBossBar()
    {
        for (Region region : bossBars.keySet())
        {
            BossBar bar = bossBars.get(region);
            CapturePoint capturePoint = captureManager.getCapturePoint(region);

            if (capturePoint == null) //TODO: replace with event listener
            {
                bar.setTitle(getName(region, "Owned"));
                bar.setColor(BarColor.BLUE);
                bar.setStyle(BarStyle.SOLID);
                continue;
            }

            if (capturePoint.isEnded()) //Locked point
            {
                bar.setStyle(BarStyle.SEGMENTED_6);
                bar.setColor(BarColor.WHITE);
                bar.setTitle(getName(region, ChatColor.GRAY + "Locked"));
                bar.setProgress(capturePoint.getExpirationTimeAsPercentage());
                continue;
            }
            bar.setStyle(BarStyle.SEGMENTED_20);
            bar.setColor(BarColor.RED);
            bar.setProgress(capturePoint.getCaptureProgress());
            String info = getName(region, "Owned") + ChatColor.RED + " is under attack!";
            String time = Messenger.formatTimeDifferently(capturePoint.getSecondsToEndGame(), 1);
            bar.setTitle(info + ChatColor.AQUA + " " + time);
        }
    }

    public void updateViewedBossBar(Player player)
    {
        Region lastRegion = this.lastSeenRegion.get(player);
        Region region = instance.getRegionManager().getRegion(player.getLocation());
        if (lastRegion == region)
            return; //Nothing to do if the player hasn't moved regions

        //Remove player from bossbar if player is no longer in a world with capturepoints
        if (!instance.claimWorlds.contains(player.getWorld()))
        {
            if (this.lastSeenRegion.remove(player) != null)
                bossBars.get(lastRegion).removePlayer(player);
            return;
        }

        if (lastRegion != null && bossBars.containsKey(lastRegion))
        {
            //Remove player from old bossBar
            bossBars.get(lastRegion).removePlayer(player);
        }

        //No bossbar exists for the current region
        if (!bossBars.containsKey(region))
        {
            //Create bossbar and store it
            bossBars.put(region, instance.getServer().createBossBar(getName(region, "Owned"), BarColor.BLUE, BarStyle.SOLID));
        }

        lastSeenRegion.put(player, region);
        bossBars.get(region).addPlayer(player);
        player.setCompassTarget(region.getRegionCenter(false));
    }

    private String getName(Region region, String verb)
    {
        return region.getName() + " " + verb + ChatColor.RESET + " by " + getOwningClanName(region);
    }

    public String getOwningClanName(Region region)
    {
        OfflinePlayer owner = region.getOwner();
        if (owner == null)
            return "Da Wild";

        GrandPlayer grandOwner = instance.getGrandAPI().getGrandPlayerManager().getGrandPlayer(owner);
        StringBuilder builder = new StringBuilder();

        Clan clan = instance.getClanManager().getClanByPlayerUniqueId(owner.getUniqueId());
        if (clan != null)
            builder.append(clan.getColorTag() + " ");
        builder.append(grandOwner.getNameColor().toString());
        builder.append(owner.getName());
        return builder.toString();
    }
}
