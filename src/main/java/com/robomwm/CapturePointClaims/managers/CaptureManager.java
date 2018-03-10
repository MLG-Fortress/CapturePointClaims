package com.robomwm.CapturePointClaims.managers;

import com.robomwm.CapturePointClaims.CapturePoint;
import com.robomwm.CapturePointClaims.CapturePointClaims;
import com.robomwm.CapturePointClaims.Region;
import com.robomwm.CapturePointClaims.messengers.Messenger;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

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
                    capturePoint.setTicksToEndGame(20);
                    capturePoint.checkOrEndGame(instance, null);
                }
            }
        }.runTaskTimer(instance, 0L, 20L);
        return capturePoint;
    }

    //Oh wowe dat b a lot of ifs dere!!!!
    public void startOrContinueCapture(Player player, Region region)
    {
        CapturePoint capturePoint = pointsBeingCaptured.get(region); //If null, no clan is currently capturing

        if (capturePoint == null) //Start a capture
        {
            capturePoint = startNewCapture(region);

            if (capturePoint.getOwner() != null) //notify defenders
            {
                Clan defendingClan = clanManager.getClanByPlayerUniqueId(capturePoint.getOwner().getUniqueId());
                if (defendingClan != null)
                    Messenger.alertMembersOfAttack(defendingClan, region);
                //TODO: mail owner
            }

            //TODO: Fire event
            //TODO: Broadcast globally (small chat message)
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
                player.sendMessage("Point is locked, please wait " + Messenger.formatTime(capturePoint.getExpirationTimeRemaining()));
            }
        }
        else if (instance.getRegionManager().isEnemyClaim(region, player, true)) //Continue capture
        {
            player.sendActionBar(ChatColor.AQUA + "Capture point health: " + capturePoint.decrementCaptureProgress(1));
            capturePoint.checkOrEndGame(instance, player);
        }
        else if (!instance.getRegionManager().isEnemyClaim(region, player, true))
        {
            player.sendMessage("Defend this point until the timer runs out!");
        }
        else
            instance.getLogger().severe("Bad thing happened in startOrContinueCapture method");
    }
}

