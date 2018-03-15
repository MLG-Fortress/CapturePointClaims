package com.robomwm.CapturePointClaims.listeners;

import com.robomwm.CapturePointClaims.CapturePointClaims;
import com.robomwm.CapturePointClaims.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Created on 2/2/2018.
 *
 * @author RoboMWM
 */
public class PointUpgrader implements Listener
{
    private CapturePointClaims instance;

    public PointUpgrader(CapturePointClaims plugin)
    {
        instance = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Region region = instance.getRegionManager().getRegion(block.getLocation());

        if (region == null)
            return;
        if (!region.nearRegionPost(block.getLocation(), 0) || block.getType() != Material.BEACON)
            return;

        event.setCancelled(true);

        if (instance.getRegionManager().isEnemyClaim(region, player, true))
            return;
        if (instance.getCaptureManager().getCapturePoint(region) != null && !instance.getCaptureManager().getCapturePoint(region).isEnded())
            return;

        player.openInventory(instance.getServer().createInventory(new UpgradeInventoryHolder(region), 54, "Deposit upgrades. " + ChatColor.DARK_BLUE + "/help post"));
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerUpgrades(InventoryCloseEvent event)
    {
        if (!(event.getInventory().getHolder() instanceof UpgradeInventoryHolder))
            return;
        Region region = ((UpgradeInventoryHolder)event.getInventory().getHolder()).getRegion();

        for (ItemStack itemStack : event.getInventory())
        {
            if (itemStack == null)
                continue;

            switch (itemStack.getType())
            {
                case REDSTONE_BLOCK:
                    region.addFuel(itemStack.getAmount());
                    break;
                case EMERALD_BLOCK:
                    region.addHealth(itemStack.getAmount());
                    break;
                case PRISMARINE:
                    region.addFatigue(itemStack.getAmount());
                    break;
                case IRON_BLOCK:
                    region.addGolem(itemStack.getAmount());
                    break;
                case GOLD_BLOCK:
                    region.addZerg(itemStack.getAmount());
                    break;
                default:
                    event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), itemStack);
            }
        }

        region.getRegionManager().saveRegion(region);
        event.getPlayer().sendMessage("Post " + region.getName() + " :\n" +
                "  Health: " + region.getHealth() +
                "\n  Fuel: " + region.getFuel() +
                "\n  Fatigue: " + region.getFatigue() +
                "\n  Sentry ammo: " + region.getArrows() +
                "\n  Golems recruited: " + region.getGolems() / 3 +
                "\n  Zerg nest size: " + region.getZerg());
    }
}

class UpgradeInventoryHolder implements InventoryHolder
{
    private Region region;

    UpgradeInventoryHolder(Region region)
    {
        this.region = region;
    }

    public Region getRegion()
    {
        return region;
    }

    @Override
    public Inventory getInventory()
    {
        return Bukkit.createInventory(null, 54);
    }
}

