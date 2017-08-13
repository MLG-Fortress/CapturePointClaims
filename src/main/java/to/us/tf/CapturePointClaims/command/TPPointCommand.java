package to.us.tf.CapturePointClaims.command;

import me.robomwm.BetterTPA.BetterTPA;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import to.us.tf.CapturePointClaims.Region;
import to.us.tf.CapturePointClaims.managers.RegionManager;

/**
 * Created on 7/7/2017.
 *
 * @author RoboMWM
 */
public class TPPointCommand implements CommandExecutor
{
    JavaPlugin instance;
    ClanManager clansManager;
    RegionManager regionManager;
    BetterTPA betterTPA;

    public TPPointCommand(JavaPlugin plugin, ClanManager clansManager, RegionManager regionManager)
    {
        this.instance = plugin;
        this.clansManager = clansManager;
        this.regionManager = regionManager;
        this.betterTPA = (BetterTPA)plugin.getServer().getPluginManager().getPlugin("BetterTPA");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage("No console variant available for this command.");
            return false;
        }

        Player player = (Player)sender;
        ClanPlayer clanPlayer = clansManager.getClanPlayer(player);

        if (clanPlayer == null)
        {
            sender.sendMessage(ChatColor.RED + "You need to be part of a clan to teleport to clan-claimed capture points."); //TODO: instructions to /join
            return false;
        }

        if (args.length < 3)
        {
            errorMessage(player, clanPlayer);
            return false;
        }

        World world = instance.getServer().getWorld(args[0].toLowerCase());
        int x;
        int z;

        try
        {
            x = Integer.parseInt(args[1]);
            z = Integer.parseInt(args[2]);
        }
        catch (Exception e)
        {
            sender.sendMessage();
            return false;
        }

        Region region = regionManager.getRegion(world, x, z);
        if (region == null || region.getOwningClanTag() == null || !region.getOwningClanTag().equals(clanPlayer.getClan().getTag()))
        {
            errorMessage(player, clanPlayer);
            sender.sendMessage(ChatColor.RED + "Invalid point, or not claimed by your clan.");
            return false;
        }

        betterTPA.teleportPlayer(player, region.getName(), region.getRegionCenter(true).add(2, 0, 2), true, null);
        return true;
    }

    private void errorMessage(Player player, ClanPlayer clanPlayer)
    {
        if (clanPlayer != null)
        {
            for (Region region : regionManager.getRegions(clanPlayer.getClan().getTag()))
            {
                player.sendMessage(region.getName());
            }
        }
        player.sendMessage(ChatColor.GOLD + "/tppoint <world> <x> <z>");
    }
}
