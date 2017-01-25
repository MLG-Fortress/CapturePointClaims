package to.us.tf.CapturePointClaims;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.Chunk;
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
    ClanManager clanManager;
    RegionManager regionManager;

    public void onEnable()
    {
        saveConfig(); //Create data folder
        regionManager = new RegionManager(this);
        SimpleClans sc = (SimpleClans)getServer().getPluginManager().getPlugin("SimpleClans");
        this.clanManager = sc.getClanManager();
        getServer().getPluginManager().registerEvents(this, this);
        claimWorlds.add(getServer().getWorld("world"));
        claimWorlds.add(getServer().getWorld("cityworld"));
        claimWorlds.add(getServer().getWorld("cityworld_nether"));
        claimWorlds.add(getServer().getWorld("world_nether"));
        CaptureManager captureManager = new CaptureManager(this, clanManager, regionManager);
        getServer().getPluginManager().registerEvents(new BlockEventListener(this, captureManager, clanManager, regionManager), this);
        new BossBarMessenger(this, captureManager, regionManager);
    }

    public void onDisable()
    {
        regionManager.saveData(this);
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
        Region region = regionManager.fromLocation(lesserCorner);
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

    /**
     * Utils (should be in a static class of its own????)
     */

    /**
     * @param region
     * @return clan that owns region, otherwise null
     */
    public Clan getOwningClan(Region region)
    {
        if (region.getOwningClanTag() == null)
            return null;
        return clanManager.getClan(region.getOwningClanTag());
    }

    public String getOwningClanString(Region region)
    {
        Clan clan = getOwningClan(region);
        if (clan == null)
            return "Wilderness";
        return clan.getName();
    }
}
