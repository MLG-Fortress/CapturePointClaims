package to.us.tf.CapturePointClaims;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    void onBlockBreak(BlockBreakEvent event)
    {
        Player player = event.getPlayer();

        Block block = event.getBlock();

        //if the player is not in managed world, do nothing (let vanilla code and other plugins do whatever)
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
            //Else start claiming process
            else
        }
        //Otherwise, just general region claim check stuff
        else if (isEnemyClaim(blockRegion, player))
        {
            //Cancel if item is not a tool, otherwise reduce durability of tool
            if (player.getInventory().getItemInMainHand().getDurability() > 0)
            {}
        }
    }

    void onBlockPlace(BlockPlaceEvent event)
    {
        Location location = event.getBlock().getLocation();

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
