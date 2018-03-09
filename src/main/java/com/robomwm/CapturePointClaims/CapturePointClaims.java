package com.robomwm.CapturePointClaims;

import com.robomwm.grandioseapi.GrandioseAPI;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.robomwm.CapturePointClaims.command.TPPointCommand;
import com.robomwm.CapturePointClaims.listeners.BlockEventListener;
import com.robomwm.CapturePointClaims.listeners.PointUpgrader;
import com.robomwm.CapturePointClaims.managers.CaptureManager;
import com.robomwm.CapturePointClaims.managers.RegionManager;
import com.robomwm.CapturePointClaims.messengers.BossBarMessenger;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by RoboMWM on 10/22/2016.
 */
public class CapturePointClaims extends JavaPlugin implements Listener
{
    public Set<World> claimWorlds = new HashSet<>();
    private ClanManager clanManager;
    private RegionManager regionManager;
    private CaptureManager captureManager;

    public RegionManager getRegionManager()
    {
        return regionManager;
    }
    public ClanManager getClanManager()
    {
        return clanManager;
    }
    public CaptureManager getCaptureManager()
    {
        return captureManager;
    }
    public GrandioseAPI getGrandAPI()
    {
        return (GrandioseAPI)getServer().getPluginManager().getPlugin("GrandioseAPI");
    }

    public void onEnable()
    {
        saveConfig(); //Create data folder
        SimpleClans sc = (SimpleClans) getServer().getPluginManager().getPlugin("SimpleClans");
        this.clanManager = sc.getClanManager();
        claimWorlds.add(getServer().getWorld("world"));
        claimWorlds.add(getServer().getWorld("cityworld"));
        this.regionManager = new RegionManager(this);

        getServer().getPluginManager().registerEvents(this, this);
        captureManager = new CaptureManager(this, clanManager, regionManager);
        getServer().getPluginManager().registerEvents(new BlockEventListener(this, captureManager, clanManager, regionManager), this);
        new BossBarMessenger(this, captureManager);
        getCommand("tppoint").setExecutor(new TPPointCommand(this, clanManager, regionManager));
        new PointUpgrader(this);
    }

    @EventHandler
    void onChunkLoad(ChunkLoadEvent event)
    {
        World world = event.getWorld();
        if (!claimWorlds.contains(world))
            return;

        //find the boundaries of the chunk
        Chunk chunk = event.getChunk();
        Location lesserCorner = chunk.getBlock(0, 0, 0).getLocation();
        Location greaterCorner = chunk.getBlock(15, 0, 15).getLocation();

        //find the center of this chunk's region
        Region region = regionManager.getRegion(lesserCorner);
        Location regionCenter = region.getRegionCenter(false);

        //if the chunk contains the region center
        if(	regionCenter.getBlockX() >= lesserCorner.getBlockX() && regionCenter.getBlockX() <= greaterCorner.getBlockX() &&
                regionCenter.getBlockZ() >= lesserCorner.getBlockZ() && regionCenter.getBlockZ() <= greaterCorner.getBlockZ())
        {
            CapturePointClaims instance = this;
            new BukkitRunnable()
            {
                public void run()
                {
                    region.AddRegionPost(instance);
                }
            }.runTaskLater(this, 200L);
        }
    }
}