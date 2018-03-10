package com.robomwm.CapturePointClaims.command;

import com.robomwm.CapturePointClaims.CapturePointClaims;
import com.robomwm.CapturePointClaims.Region;
import com.robomwm.CapturePointClaims.managers.RegionManager;
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

import java.util.HashSet;
import java.util.Set;

/**
 * Created on 7/7/2017.
 *
 * @author RoboMWM
 */
public class TPPointCommand implements CommandExecutor
{
    private final String TAB = "    ";

    private CapturePointClaims instance;
    private ClanManager clansManager;
    private RegionManager regionManager;
    private BetterTPA betterTPA;

    public TPPointCommand(CapturePointClaims plugin, ClanManager clansManager, RegionManager regionManager)
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

        //If world does not contain any points, not owned by any clan, or owned by an enemy clan...
        if (region == null || instance.getRegionManager().isEnemyClaim(region, player, true))
        {
            errorMessage(player, clanPlayer);
            sender.sendMessage(ChatColor.RED + "Invalid point or not claimed by you/your clan.");
            return false;
        }

        betterTPA.teleportPlayer(player, region.getName(), region.getRegionCenter(true).add(2, 0, 2), true, null);
        return true;
    }

    //TODO: make tppoints clickable to teleport
    private void errorMessage(Player player, ClanPlayer clanPlayer)
    {
        if (clanPlayer != null)
        {
            for (String alliedClanTag : clanPlayer.getClan().getAllies())
            {
                Clan alliedClan = clansManager.getClan(alliedClanTag);
                player.sendMessage(alliedClan.getColorTag() + "'s points: ");
                player.sendMessage(formattedSet(regionNames(regionManager.getRegions(alliedClan)), ChatColor.GREEN));
            }
            player.sendMessage(clanPlayer.getClan().getColorTag() + "'s points: ");
            player.sendMessage(formattedSet(regionNames(regionManager.getRegions(clanPlayer.getClan())), ChatColor.AQUA));
        }
        else
            player.sendMessage(formattedSet(regionNames(regionManager.getRegions(player)), ChatColor.AQUA));
        player.sendMessage(ChatColor.GOLD + "/tppoint <world> <x> <z>");
    }

    private String formattedSet(Set<String> stringSet, ChatColor color)
    {
        int i = 0;
        StringBuilder formattedString = new StringBuilder(color.toString());
        for (String string : stringSet)
        {
            if (i > 0)
            {
                if (i % 2 == 0)
                    formattedString.append("\n" + color);
                else
                    formattedString.append(TAB);
            }
            formattedString.append(string);
            i++;
        }
        return formattedString.toString();
    }

    private Set<String> regionNames(Set<Region> regions)
    {
        Set<String> whatever = new HashSet<>();
        for (Region region : regions)
            whatever.add(region.getName());
        return whatever;
    }
}
