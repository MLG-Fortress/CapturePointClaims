package to.us.tf.CapturePointClaims.listeners;

import org.bukkit.Bukkit;
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
import to.us.tf.CapturePointClaims.CapturePointClaims;
import to.us.tf.CapturePointClaims.Region;

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
        if (instance.isEnemyClaim(region, player, true))
            return;
        if (instance.getCaptureManager().getCapturePoint(region) != null && !instance.getCaptureManager().getCapturePoint(region).isEnded())
            return;
        if (!region.nearRegionPost(block.getLocation(), 1) || block.getType() != Material.EMERALD_BLOCK)
            return;

        Inventory inventory = instance.getServer().createInventory(new UpgradeInventoryHolder(region), 54, "Input upgrades here. See /help capturepoint");
        player.openInventory(inventory);
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerUpgrades(InventoryCloseEvent event)
    {
        if (!(event.getInventory().getHolder() instanceof UpgradeInventoryHolder))
            return;
        Region region = ((UpgradeInventoryHolder)event.getInventory()).getRegion();

        int emeraldBlocks = region.getCaptureTime();
        int diamondBlocks = region.getHealth();

        for (ItemStack itemStack : event.getInventory())
        {
            switch (itemStack.getType())
            {
                case DIAMOND_BLOCK:
                    diamondBlocks += itemStack.getAmount();
                    break;
                case EMERALD_BLOCK:
                    if (emeraldBlocks > 5)
                    {
                        int amount = itemStack.getAmount();
                        if (emeraldBlocks - amount < 5)
                        {
                            amount = emeraldBlocks - 5;
                            itemStack.setAmount(itemStack.getAmount() - amount);
                            event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), itemStack);
                        }
                        emeraldBlocks -= amount;
                        break;
                    }
                    default:
                        event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), itemStack);
            }
        }

        region.setHealth(diamondBlocks);
        region.setCaptureTime(emeraldBlocks);
        event.getPlayer().sendMessage("Point " + region.toString() + ": v\nulnerability window: " + region.getCaptureTime() + " minutes.\nHealth: " + region.getHealth());
    }
}

class UpgradeInventoryHolder implements InventoryHolder
{
    Region region;

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

