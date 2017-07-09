package to.us.tf.CapturePointClaims.managers;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import to.us.tf.CapturePointClaims.CapturePoint;
import to.us.tf.CapturePointClaims.CapturePointClaims;
import to.us.tf.CapturePointClaims.Region;
import to.us.tf.CapturePointClaims.messengers.Messenger;

import java.util.*;

/**
 * Created by robom on 12/20/2016.
 */
public class CaptureManager
{
    ClanManager clanManager;
    //Set of CapturePoints being captured
    private Map<Region, CapturePoint> pointsBeingCaptured = new HashMap<>();
    CapturePointClaims instance;
    RegionManager regionManager;

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

    private CapturePoint startNewCapture(Clan attackingClan, Region region)
    {
        CapturePoint capturePoint = new CapturePoint(attackingClan, instance.getOwningClan(region), region);
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
                    capturePoint.checkOrEndGame(instance);
                }
            }
        }.runTaskTimer(instance, 0L, 20L);
        return capturePoint;
    }

    //Oh wowe dat b a lot of ifs dere!!!!
    public void startOrContinueCapture(Player player, Region region)
    {
        CapturePoint capturePoint = pointsBeingCaptured.get(region); //If null, no clan is currently capturing
        Clan clan = clanManager.getClanByPlayerUniqueId(player.getUniqueId());

        if (clan == null)
        {
            player.sendMessage(ChatColor.RED + "You need to be part of a clan to capture a point.");
            return;
        }

        if (capturePoint == null) //Start a capture
        {
            capturePoint = startNewCapture(clan, region);

            if (capturePoint.getOwningClan() != null) //notify defenders
            {
                Clan defendingClan = capturePoint.getOwningClan();
                Messenger.alertMembersOfAttack(clan, capturePoint.getOwningClan(), region);
                //TODO: Dynmap: make claim appear in red or some color no clan uses
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
        else if (capturePoint.getAttackingClan() != clan) //Another clan is already capturing
        {
            player.sendMessage("Point is being captured by " + capturePoint.getAttackingClan().getColorTag());
        }
        else if (capturePoint.getAttackingClan() == clan) //Continue capture
        {
            player.sendActionBar(ChatColor.AQUA + "Capture point health: " + capturePoint.decrementCaptureProgress(1) + "/100");
            capturePoint.checkOrEndGame(instance);
        }
        else
            instance.getLogger().severe("Bad thing happened in startOrContinueCapture method");
    }
}
