package com.robomwm.CapturePointClaims.point;

import com.robomwm.CapturePointClaims.CapturePointClaims;
import com.robomwm.CapturePointClaims.events.CaptureFinishedEvent;
import com.robomwm.CapturePointClaims.events.CaptureStartEvent;
import com.robomwm.CapturePointClaims.region.Region;
import com.robomwm.CapturePointClaims.region.RegionManager;
import com.robomwm.CapturePointClaims.messengers.Messenger;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by robom on 12/20/2016.
 */
public class CaptureManager
{
    private ClanManager clanManager;
    //Set of CapturePoints being captured
    private Map<Region, CapturePoint> pointsBeingCaptured = new HashMap<>();
    private CapturePointClaims instance;
    private RegionManager regionManager;

    public CaptureManager(CapturePointClaims capturePointClaims, ClanManager clanManager, RegionManager regionManager)
    {
        this.clanManager = clanManager;
        this.instance = capturePointClaims;
        this.regionManager = regionManager;
    }

    public CapturePoint getCapturePoint(Region region)
    {
        //Remove expired capture points
        CapturePoint capturePoint = pointsBeingCaptured.get(region);
        if (capturePoint != null && capturePoint.isLockExpired())
            pointsBeingCaptured.remove(region);

        return pointsBeingCaptured.get(region);
    }

    private CapturePoint startNewCapture(Region region)
    {
        CapturePoint capturePoint = new CapturePoint(region);
        pointsBeingCaptured.put(region, capturePoint);
        new BukkitRunnable()
        {
            public void run()
            {
                if (capturePoint.isEnded())
                    this.cancel();
                else
                {
                    capturePoint.tick(20);
                    checkOrEndGame(capturePoint, null);
                }
            }
        }.runTaskTimer(instance, 0L, 20L);
        instance.getServer().getPluginManager().callEvent(new CaptureStartEvent(capturePoint));
        return capturePoint;
    }

    //Oh wowe dat b a lot of ifs dere!!!!
    public void startOrContinueCapture(Player player, Region region)
    {
        CapturePoint capturePoint = pointsBeingCaptured.get(region); //If null, no clan is currently capturing

        if (capturePoint == null) //Start a capture
        {
            startNewCapture(region);

            //notify defenders
            Messenger.mailPlayerOrClan(instance, region.getOwner(), region.getName() + " is under attack!");
        }

        else if (capturePoint.isEnded()) //Point was already captured/defended before
        {
            if (capturePoint.isLockExpired())
            {
                //Start a new capture
                pointsBeingCaptured.remove(region);
                startOrContinueCapture(player, region);
            }
            else
            {
                player.sendMessage("Post is locked, please wait " + Messenger.formatTime(capturePoint.getExpirationTimeRemaining()));
            }
        }
        else if (instance.getRegionManager().isEnemyClaim(region, player, true)) //Continue capture
        {
            player.sendActionBar(ChatColor.DARK_RED + "Post health: " + capturePoint.decrementCaptureProgress(1));
            checkOrEndGame(capturePoint, player);
        }
        else if (!instance.getRegionManager().isEnemyClaim(region, player, true))
        {
            player.sendMessage("Defend this post until the timer runs out!");
        }
        else
            instance.getLogger().severe("Bad thing happened in startOrContinueCapture method");
    }

    private void checkOrEndGame(CapturePoint point, Player attacker)
    {
        Boolean captured = point.checkOrEndGame(instance, attacker);
        if (captured == null)
            return;
        if (!captured)
            return;

        Messenger.mailPlayerOrClan(instance, attacker, "Successfully captured " + point.getRegion().getName());
        Messenger.mailPlayerOrClan(instance, point.getDefender(), point.getRegion().getName() + " was captured by " + attacker.getName());
        instance.getServer().getPluginManager().callEvent(new CaptureFinishedEvent(point, attacker));
    }
}

