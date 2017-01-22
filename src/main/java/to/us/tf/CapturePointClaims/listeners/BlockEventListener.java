package to.us.tf.CapturePointClaims.listeners;

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
import to.us.tf.CapturePointClaims.CapturePointClaims;
import to.us.tf.CapturePointClaims.CaptureManager;
import to.us.tf.CapturePointClaims.RegionCoordinates;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by RoboMWM on 1/21/2017.
 */
public class BlockEventListener implements Listener
{
    CapturePointClaims instance;
    CaptureManager captureManager;
    ClanManager clanManager;
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

    public BlockEventListener(CapturePointClaims capturePointClaims, CaptureManager captureManager, ClanManager clanManager)
    {
        this.instance = capturePointClaims;
        this.captureManager = captureManager;
        this.clanManager = clanManager;
    }

    private boolean isEnemyClaim(RegionCoordinates regionCoordinates, Player player, boolean includeWildernessAsEnemy)
    {
        Clan clan = instance.getOwningClan(regionCoordinates);
        Clan playerClan = clanManager.getClanByPlayerUniqueId(player.getUniqueId());

        if (clan == null) //Unclaimed
        {
            return includeWildernessAsEnemy;
        }

        return playerClan != clan;
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
                player.sendActionBar(ChatColor.RED + "Use a tool to break blocks in an enemy claim.");
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
