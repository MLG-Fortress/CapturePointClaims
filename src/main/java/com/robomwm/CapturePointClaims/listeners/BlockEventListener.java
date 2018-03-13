package com.robomwm.CapturePointClaims.listeners;

import com.robomwm.CapturePointClaims.CapturePointClaims;
import com.robomwm.CapturePointClaims.Region;
import com.robomwm.CapturePointClaims.managers.RegionManager;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.InventoryHolder;
import com.robomwm.CapturePointClaims.managers.CaptureManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by RoboMWM on 1/21/2017.
 */
public class BlockEventListener implements Listener
{
    CapturePointClaims instance;
    CaptureManager captureManager;
    ClanManager clanManager;
    RegionManager regionManager;
    private Set<Material> alwaysBreakableMaterials = new HashSet<Material>(Arrays.asList(
            Material.LONG_GRASS,
            Material.DOUBLE_PLANT,
            Material.LEAVES,
            Material.LEAVES_2,
            Material.RED_ROSE,
            Material.YELLOW_FLOWER,
            Material.SNOW_BLOCK
    ));

    public BlockEventListener(CapturePointClaims capturePointClaims, CaptureManager captureManager, ClanManager clanManager, RegionManager regionManager)
    {
        this.instance = capturePointClaims;
        this.captureManager = captureManager;
        this.clanManager = clanManager;
        this.regionManager = regionManager;
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
        Region blockRegion = regionManager.getRegion(blockLocation);

        //if too close to (or above) region post,
        if(blockRegion.nearRegionPost(blockLocation, 2))
        {
            event.setCancelled(true);
            if (instance.getRegionManager().isEnemyClaim(blockRegion, player, true))
                captureManager.startOrContinueCapture(player, blockRegion); //start/continue claiming process
            else //player's clan already claimed this, do nothing more
                player.sendMessage(ChatColor.RED + "You/your clan already captured this post. You can upgrade it though; see " + ChatColor.GOLD + "/help post");
        }
        //Otherwise, just general region claim check stuff
        else if (instance.getRegionManager().isEnemyClaim(blockRegion, player, false)
                && !isTool(player.getInventory().getItemInMainHand().getType()))
        {
            event.setCancelled(true);
            player.sendActionBar(ChatColor.RED + "Use a tool to break blocks in an enemy's claim.");
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onOpenChest(PlayerInteractEvent event)
    {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        //if the player is not in managed world, do nothing
        if(!instance.claimWorlds.contains(player.getWorld()))
            return;
        if (!instance.getRegionManager().isEnemyClaim(block.getLocation(), player, false))
            return;

        if (block.getState() instanceof InventoryHolder)
        {
            player.sendMessage(ChatColor.RED + "Seems like this is locked by the claim post...");
            event.setCancelled(true);
        }
    }

    private boolean isTool(Material material)
    {
        switch(material)
        {
            case WOOD_SPADE:
            case WOOD_HOE:
            case WOOD_PICKAXE:
            case WOOD_SWORD:
            case WOOD_AXE:
            case STONE_SPADE:
            case STONE_HOE:
            case STONE_PICKAXE:
            case STONE_SWORD:
            case STONE_AXE:
            case GOLD_SPADE:
            case GOLD_HOE:
            case GOLD_PICKAXE:
            case GOLD_SWORD:
            case GOLD_AXE:
            case IRON_SPADE:
            case IRON_HOE:
            case IRON_PICKAXE:
            case IRON_SWORD:
            case IRON_AXE:
            case DIAMOND_SPADE:
            case DIAMOND_HOE:
            case DIAMOND_PICKAXE:
            case DIAMOND_SWORD:
            case DIAMOND_AXE:
                return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    void onToolDamage(PlayerItemDamageEvent event)
    {
        Player player = event.getPlayer();
        if (instance.getRegionManager().isEnemyClaim(player.getLocation(), player, false))
            event.setDamage(event.getDamage() * r4nd0m(2, 10));
    }

    public int r4nd0m(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onBlockPlace(BlockPlaceEvent event)
    {
        Location blockLocation = event.getBlock().getLocation();
        Player player = event.getPlayer();

        //if the player is not in managed world, do nothing
        if(!instance.claimWorlds.contains(player.getWorld())) return;

        Region blockRegion = regionManager.getRegion(blockLocation);

        if (instance.getRegionManager().isEnemyClaim(blockRegion, player, false))
        {
            event.setCancelled(true);
            player.sendActionBar(ChatColor.RED + "You must capture the claim post to build here!");
            return;
        }

        //if too close to (or above) region post,
        if(blockRegion.nearRegionPost(blockLocation, 2))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onEntityExplode(EntityExplodeEvent event)
    {
        //if not in managed world, do nothing
        if(!instance.claimWorlds.contains(event.getEntity().getWorld())) return;

        for (Block block : new ArrayList<>(event.blockList()))
        {
            Region region = regionManager.getRegion(block.getLocation());
            if (region.nearRegionPost(block.getLocation(), 2))
                event.blockList().remove(block);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onBlockExplode(BlockExplodeEvent event)
    {
        //if not in managed world, do nothing
        if(!instance.claimWorlds.contains(event.getBlock().getWorld())) return;

        for (Block block : new ArrayList<>(event.blockList()))
        {
            Region region = regionManager.getRegion(block.getLocation());
            if (region.nearRegionPost(block.getLocation(), 2))
                event.blockList().remove(block);
        }
    }

    //Apparently I'm not allowed to listen to BlockPistonEvent... https://gist.github.com/RoboMWM/3b9c2799f6d16a66188aedb787966f9b

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPistonStuff(BlockPistonExtendEvent event)
    {
        Block block = event.getBlock();
        //if not in managed world, do nothing
        if(!instance.claimWorlds.contains(event.getBlock().getWorld())) return;

        Region region = regionManager.getRegion(block.getLocation());
        if (region.nearRegionPost(block.getLocation(), 3))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPistonStuff(BlockPistonRetractEvent event)
    {
        Block block = event.getBlock();
        //if not in managed world, do nothing
        if(!instance.claimWorlds.contains(event.getBlock().getWorld())) return;

        Region region = regionManager.getRegion(block.getLocation());
        if (region.nearRegionPost(block.getLocation(), 3))
            event.setCancelled(true);
    }
}
