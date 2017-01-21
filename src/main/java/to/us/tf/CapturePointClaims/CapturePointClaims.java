package to.us.tf.CapturePointClaims;

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
    RegionCoordinates regionCoordinates = new RegionCoordinates();
    protected Set<World> claimWorlds = new HashSet<>();
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
        new BossBarMessenger(this, regionCoordinates);
        getServer().getPluginManager().registerEvents(new CapturingManager(this, sc.getClanManager(), regionCoordinates), this);
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
        RegionCoordinates region = regionCoordinates.fromLocation(lesserCorner);
        Location regionCenter = regionCoordinates.getRegionCenter(region, false);

        //if the chunk contains the region center
        if(	regionCenter.getBlockX() >= lesserCorner.getBlockX() && regionCenter.getBlockX() <= greaterCorner.getBlockX() &&
                regionCenter.getBlockZ() >= lesserCorner.getBlockZ() && regionCenter.getBlockZ() <= greaterCorner.getBlockZ())
        {
            CapturePointClaims instance = this;
            new BukkitRunnable()
            {
                public void run()
                {
                    regionCoordinates.AddRegionPost(region, instance);
                }
            }.runTaskLater(this, 200L);

        }
    }
}
