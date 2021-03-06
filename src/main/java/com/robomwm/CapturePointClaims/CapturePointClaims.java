package com.robomwm.CapturePointClaims;

import com.robomwm.CapturePointClaims.command.TPPointCommand;
import com.robomwm.CapturePointClaims.listeners.BlockEventListener;
import com.robomwm.CapturePointClaims.listeners.PointUpgrader;
import com.robomwm.CapturePointClaims.listeners.SimpleClansListener;
import com.robomwm.CapturePointClaims.messengers.BossBarMessenger;
import com.robomwm.CapturePointClaims.point.CaptureManager;
import com.robomwm.CapturePointClaims.region.Region;
import com.robomwm.CapturePointClaims.region.RegionManager;
import com.robomwm.grandioseapi.GrandioseAPI;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.Chunk;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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

        for (World world : getServer().getWorlds())
        {
            if (world.getPVP()
                && world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)
                && world.getWorldBorder().getSize() >= 20000)
                claimWorlds.add(world);
        }

        this.regionManager = new RegionManager(this);

        getServer().getPluginManager().registerEvents(this, this);
        captureManager = new CaptureManager(this, clanManager, regionManager);
        getServer().getPluginManager().registerEvents(new BlockEventListener(this, captureManager, clanManager, regionManager), this);
        new BossBarMessenger(this, captureManager);
        getCommand("tppost").setExecutor(new TPPointCommand(this, clanManager, regionManager));
        new PointUpgrader(this);
        new SimpleClansListener(this);
        new DynmapHook(this);
    }

    @EventHandler
    private void onChunkLoad(ChunkLoadEvent event)
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
                    if (chunk.isLoaded())
                        region.AddRegionPost(instance);
                }
            }.runTaskLater(this, 200L);
        }
    }
}
