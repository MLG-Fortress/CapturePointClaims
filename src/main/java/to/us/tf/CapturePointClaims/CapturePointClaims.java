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
import to.us.tf.CapturePointClaims.listeners.BlockEventListener;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by RoboMWM on 10/22/2016.
 */
public class CapturePointClaims extends JavaPlugin implements Listener
{
    public Set<World> claimWorlds = new HashSet<>();
    ClanManager clanManager;

    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
        claimWorlds.add(getServer().getWorld("world"));
        claimWorlds.add(getServer().getWorld("cityworld"));
        claimWorlds.add(getServer().getWorld("cityworld_nether"));
        claimWorlds.add(getServer().getWorld("world_nether"));
        SimpleClans sc = (SimpleClans)getServer().getPluginManager().getPlugin("SimpleClans");
        this.clanManager = sc.getClanManager();
        CaptureManager captureManager = new CaptureManager(this, clanManager);
        getServer().getPluginManager().registerEvents(new BlockEventListener(this, captureManager, clanManager), this);
        new BossBarMessenger(this, captureManager);
    }

    public String getOwningClanString(RegionCoordinates region)
    {
        if (region.getOwningClanTag() == null)
            return "Wilderness";
        return clanManager.getClan(region.getOwningClanTag()).getName();
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
        RegionCoordinates region = new RegionCoordinates().fromLocation(lesserCorner);
        Location regionCenter = region.getRegionCenter(region, false);

        //if the chunk contains the region center
        if(	regionCenter.getBlockX() >= lesserCorner.getBlockX() && regionCenter.getBlockX() <= greaterCorner.getBlockX() &&
                regionCenter.getBlockZ() >= lesserCorner.getBlockZ() && regionCenter.getBlockZ() <= greaterCorner.getBlockZ())
        {
            CapturePointClaims instance = this;
            new BukkitRunnable()
            {
                public void run()
                {
                    region.AddRegionPost(region, instance);
                }
            }.runTaskLater(this, 200L);
        }
    }

    /**
     * Utils (should be in a static class of its own????)
     */

    /**
     * @param regionCoordinates
     * @return clan that owns region, otherwise null
     */
    public Clan getOwningClan(RegionCoordinates regionCoordinates)
    {
        if (regionCoordinates.getOwningClanTag() == null)
            return null;
        return clanManager.getClan(regionCoordinates.getOwningClanTag());
    }
}
