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
public class CapturingManager implements Listener
{
    ClanManager clanManager;
    //Set of CapturePoints being captured
    Map<RegionCoordinates, CapturePoint> pointsBeingCaptured = new HashMap<>();
    CapturePointClaims instance;
    RegionCoordinates regionCoordinates;

    private Set<Material> alwaysBreakableMaterials = new HashSet<Material>(Arrays.asList(
            Material.LONG_GRASS,
            Material.DOUBLE_PLANT,
            Material.LOG,
            Material.LOG_2,
            Material.LEAVES,
            Material.LEAVES_2,
            Material.RED_ROSE,
            Material.YELLOW_FLOWER,
            Material.SNOW_BLOCK
    ));

    public CapturingManager(CapturePointClaims capturePointClaims, ClanManager clanManager, RegionCoordinates regionCoordinates)
    {
        this.clanManager = clanManager;
        this.instance = capturePointClaims;
        this.regionCoordinates = regionCoordinates;
    }

    private boolean nearRegionPost(Location location, RegionCoordinates region, int howClose)
    {
        Location postLocation = regionCoordinates.getRegionCenter(region, false);

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

    /**
     *
     * @param regionCoordinates
     * @return clan that owns region, otherwise null
     */
    private Clan getOwningClan(RegionCoordinates regionCoordinates)
    {
        if (regionCoordinates.getOwningClanTag() == null)
            return null;
        return clanManager.getClan(regionCoordinates.getOwningClanTag());
    }

    private boolean isEnemyClaim(RegionCoordinates regionCoordinates, Player player, boolean includeWildernessAsEnemy)
    {
        Clan clan = getOwningClan(regionCoordinates);
        Clan playerClan = clanManager.getClanByPlayerUniqueId(player.getUniqueId());

        if (clan == null) //Unclaimed
        {
            return includeWildernessAsEnemy;
        }

        return playerClan != clan;
    }

    private CapturePoint startNewCapture(Clan attackingClan, RegionCoordinates regionCoordinates)
    {
        CapturePoint capturePoint = new CapturePoint(attackingClan, getOwningClan(regionCoordinates));
        pointsBeingCaptured.put(regionCoordinates, capturePoint);
        new BukkitRunnable()
        {
            public void run()
            {
                if (capturePoint.setTicksToEndGame(20))
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
            if (capturePoint.getTimeCaptured() < System.currentTimeMillis() - 1440000) //Over a day
            {
                //Start a new capture
                pointsBeingCaptured.remove(regionCoordinates);
                startOrContinueCapture(player, regionCoordinates);
            }
            else
            {
                player.sendMessage("Point is locked, please wait " + Messenger.formatTime(capturePoint.getTimeCaptured() - (System.currentTimeMillis() - 1440000)));
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

    @EventHandler(ignoreCancelled = true)
    void onBlockBreak(BlockBreakEvent event)
    {
        Player player = event.getPlayer();

        Block block = event.getBlock();

        //if the player is not in managed world, do nothing
        if(!instance.claimWorlds.contains(player.getWorld())) return;

        //whitelist for blocks which can always be broken (grass cutting, tree chopping)
        if(this.alwaysBreakableMaterials.contains(block.getType())) return;

        //otherwise figure out which region that block is in
        Location blockLocation = block.getLocation();

        RegionCoordinates blockRegion = regionCoordinates.fromLocation(blockLocation);

        //if too close to (or above) region post,
        if(this.nearRegionPost(blockLocation, blockRegion, 2))
        {
            event.setCancelled(true);
            if (!isEnemyClaim(blockRegion, player, true)) //player's clan already claimed this, do nothing more
                return;
            //Otherwise start/continue claiming process
            else
                startOrContinueCapture(player, regionCoordinates);
        }
        //Otherwise, just general region claim check stuff
        else if (isEnemyClaim(blockRegion, player, false))
        {
            short durability = player.getInventory().getItemInMainHand().getDurability();
            //TODO: Cancel if item is not a tool
            if (durability > 0)
            {
                event.setCancelled(true);
                player.sendActionBar(ChatColor.RED + "Use a tool to damage blocks in an enemy claim.");
                return;
            }
            //TODO: otherwise reduce durability of tool
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onBlockPlace(BlockPlaceEvent event)
    {
        Location blockLocation = event.getBlock().getLocation();
        Player player = event.getPlayer();

        //if the player is not in managed world, do nothing
        if(!instance.claimWorlds.contains(player.getWorld())) return;

        RegionCoordinates blockRegion = regionCoordinates.fromLocation(blockLocation);

        if (isEnemyClaim(blockRegion, player, false))
        {
            event.setCancelled(true);
            player.sendActionBar(ChatColor.RED + "First capture this area before building here.");
            return;
        }

        //if too close to (or above) region post,
        if(this.nearRegionPost(blockLocation, blockRegion, 2))
        {
            event.setCancelled(true);
        }
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

    //Capturing a new point
    public CapturePoint(Clan attackingClan, Clan owningClan)
    {
        this.attackingClan = attackingClan;
        this.owningClan = owningClan;
    }

    public Clan getAttackingClan()
    {
        return this.attackingClan;
    }

    public Clan getOwningClan()
    {
        return this.owningClan;
    }

    public String getOwningClanTag()
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

    /**
     * @param ticksToTick
     * @return if capture time is up
     */
    public boolean setTicksToEndGame(int ticksToTick)
    {
        this.ticksToEndGame = this.ticksToEndGame - ticksToTick;
        if (this.ticksToEndGame <= 0 || this.captureProgress <= 0)
        {
            this.timeCaptured = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public int decrementCaptureProgress(int captureProgress)
    {
        this.captureProgress -= captureProgress;
        return this.captureProgress;
    }
}
