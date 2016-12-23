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
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;

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
        Location postLocation = PopulationDensity.getRegionCenter(region, false);

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
     * @return clan that claims region, otherwise null
     */
    private Clan getClaimedClan(RegionCoordinates regionCoordinates)
    {

    }

    private boolean isEnemyClaim(RegionCoordinates regionCoordinates, Player player)
    {

    }

    private boolean startOrContinueCapture(Player player, RegionCoordinates regionCoordinates, )
    {
        Clan clan = clanManager.getClanByPlayerUniqueId(player.getUniqueId());
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
            //If player's clan already claimed this, do nothing more
            if (!isEnemyClaim(blockRegion, player))
                return;
            //TODO: Else handoff to captureManager to start/continue claiming process
            else
        }
        //Otherwise, just general region claim check stuff
        else if (isEnemyClaim(blockRegion, player))
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

        if (isEnemyClaim(blockRegion, player))
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
    //capturingClan
    Clan attackingClan;
    //defendingClan (owning clan to notify, generally)
    Clan owningClan;
    //captureProgress

    //Capturing a new point
    public CapturePoint(Clan attackingClan, Clan owningClan)
    {
        this.attackingClan = attackingClan;
        this.owningClan = owningClan;
    }
}
