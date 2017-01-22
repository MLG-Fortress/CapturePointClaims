package to.us.tf.CapturePointClaims;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Created by robom on 12/20/2016.
 */
public class CaptureManager
{
    ClanManager clanManager;
    //Set of CapturePoints being captured
    Map<RegionCoordinates, CapturePoint> pointsBeingCaptured = new HashMap<>();
    CapturePointClaims instance;
    RegionCoordinates regionCoordinates = new RegionCoordinates();



    public CaptureManager(CapturePointClaims capturePointClaims, ClanManager clanManager)
    {
        this.clanManager = clanManager;
        this.instance = capturePointClaims;
    }

    private boolean nearRegionPost(Location location, RegionCoordinates region, int howClose)
    {
        Location postLocation = region.getRegionCenter(region, false);

        //NOTE!  Why not use distance?  Because I want a box to the sky, not a sphere.
        //Why not round?  Below calculation is cheaper than distance (needed for a cylinder or sphere).
        //Why to the sky?  Because if somebody builds a platform above the post, folks will teleport onto that platform by mistake.
        //Also...  lava from above would be bad.
        //Why not below?  Because I can't imagine mining beneath a post as an avenue for griefing.

        return (	location.getBlockX() >= postLocation.getBlockX() - howClose &&
                location.getBlockX() <= postLocation.getBlockX() + howClose &&
                location.getBlockZ() >= postLocation.getBlockZ() - howClose &&
                location.getBlockZ() <= postLocation.getBlockZ() + howClose &&
                location.getBlockY() >= location.getWorld().getHighestBlockYAt(postLocation) - 4
        );
    }



    private CapturePoint startNewCapture(Clan attackingClan, RegionCoordinates regionCoordinates)
    {
        CapturePoint capturePoint = new CapturePoint(attackingClan, instance.getOwningClan(regionCoordinates), regionCoordinates);
        pointsBeingCaptured.put(regionCoordinates, capturePoint);
        new BukkitRunnable()
        {
            public void run()
            {
                capturePoint.setTicksToEndGame(20);
                if (capturePoint.isEnded())
                    this.cancel();
            }
        }.runTaskTimer(instance, 0L, 20L);
        return capturePoint;
    }

    //Oh wowe dat b a lot of ifs dere!!!!
    private void startOrContinueCapture(Player player, RegionCoordinates regionCoordinates)
    {
        CapturePoint capturePoint = pointsBeingCaptured.get(regionCoordinates); //If null, no clan is currently capturing
        Clan clan = clanManager.getClanByPlayerUniqueId(player.getUniqueId());

        if (clan == null)
        {
            player.sendMessage(ChatColor.RED + "You need to be part of a clan to capture a point.");
            return;
        }

        if (capturePoint == null) //Start a capture
        {
            capturePoint = startNewCapture(clan, regionCoordinates);

            if (capturePoint.getOwningClan() != null) //notify defenders
            {
                Clan defendingClan = capturePoint.getOwningClan();
                Messenger.alertMembersOfAttack(clan, capturePoint.getOwningClan(), regionCoordinates);
                //TODO: Dynmap: make claim appear in red or some color no clan uses
            }

            //TODO: Fire event
            //TODO: Broadcast globally (small chat message)
        }

        else if (capturePoint.isEnded()) //Point was already captured/defended before
        {
            if (capturePoint.isExpired())
            {
                //Start a new capture
                pointsBeingCaptured.remove(regionCoordinates);
                startOrContinueCapture(player, regionCoordinates);
            }
            else
            {
                player.sendMessage("Point is locked, please wait " + Messenger.formatTime(capturePoint.getExpirationTime());
            }
        }
        else if (capturePoint.getAttackingClan() != clan) //Another clan is already capturing
        {
            player.sendMessage("Point is being captured by " + capturePoint.getAttackingClan().getColorTag());
        }
        else if (capturePoint.getAttackingClan() == clan) //Continue capture
        {
            player.sendActionBar("Capture point health: " + capturePoint.decrementCaptureProgress(1) + "/100");
        }
        else
            instance.getLogger().severe("Bad thing happened in startOrContinueCapture method");
    }
}

class CapturePoint
{
    /**
     * Contains information about a point in the process of being captured
     * Automatically manages and performs end-game (times runs out or point being captured)
     */
    //capturingClan
    private Clan attackingClan;
    //defendingClan (owning clan to notify, generally. Null if unclaimed)
    private Clan owningClan;
    //captureProgress (decrements to 0)
    private int captureProgress = 100;
    //Maximum amount of time to capture point, in ticks
    private int ticksToEndGame = 6000; //5 minutes
    //Used to determine end game, and when point should be unlocked
    private Long timeCaptured = 0L;
    //Determines who won
    private boolean defended = true;
    RegionCoordinates region;

    //Capturing a new point
    public CapturePoint(Clan attackingClan, Clan owningClan, RegionCoordinates region)
    {
        this.attackingClan = attackingClan;
        this.owningClan = owningClan;
        this.region = region;
    }

    public Clan getAttackingClan()
    {
        return this.attackingClan;
    }

    public Clan getOwningClan()
    {
        return this.owningClan;
    }

    public String getOwningClanColorTag()
    {
        if (getOwningClan() == null)
            return "Wilderness";
        return getOwningClan().getColorTag();
    }

    public boolean isEnded()
    {
        return this.timeCaptured > 0L;
    }

    public Long getTimeCaptured()
    {
        return this.timeCaptured;
    }

    public int getCaptureProgress()
    {
        return this.captureProgress;
    }

    public int getTicksToEndGame()
    {
        return this.ticksToEndGame;
    }

    public int getSecondsToEndGame()
    {
        return this.ticksToEndGame / 20;
    }

    public boolean isExpired()
    {
        return this.getTimeCaptured() < (System.currentTimeMillis() - 1440000); //This is 24 minutes, we want 24 hours
    }

    public Long getExpirationTime()
    {
        return (this.getTimeCaptured() - (System.currentTimeMillis() - 1440000)) / 1000;
    }


    public void setTicksToEndGame(int ticksToTick)
    {
        if (isEnded())
            return;
        this.ticksToEndGame = this.ticksToEndGame - ticksToTick;
        checkEndGame(true);
    }

    public int decrementCaptureProgress(int captureProgress)
    {
        if (isEnded())
            return this.captureProgress;
        this.captureProgress -= captureProgress;
        checkEndGame(false);
        return this.captureProgress;
    }

    private boolean checkEndGame(boolean defenderWin)
    {
        if (this.ticksToEndGame <= 0 || this.captureProgress <= 0)
        {
            //TODO: Fire EndCaptureEvent
            this.timeCaptured = System.currentTimeMillis();
            this.defended = defenderWin;
            if (!defenderWin)
                region.setOwningClanTag(attackingClan.getTag());
            return true;
        }
        return false;
    }
}
