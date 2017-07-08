package to.us.tf.CapturePointClaims.managers;

/*
    PopulationDensity Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import to.us.tf.CapturePointClaims.CapturePointClaims;
import to.us.tf.CapturePointClaims.Region;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RegionManager
{
    final int REGION_SIZE = 400;
    YamlConfiguration regionStorage;
    Map<World, Table<Integer, Integer, Region>> worldCache = new HashMap<>();

    public RegionManager(CapturePointClaims capturePointClaims)
    {
        File storageFile = new File(capturePointClaims.getDataFolder(), "regionStorage.data");
        if (!storageFile.exists())
        {
            try
            {
                storageFile.createNewFile();
            }
            catch (IOException e)
            {
                capturePointClaims.getLogger().severe("Could not create regionStorage.data! Since I'm lazy, there currently is no \"in memory\" option. Will now disable along with a nice stack trace for you to bother me with:");
                e.printStackTrace();
                return;
            }
        }
        regionStorage = YamlConfiguration.loadConfiguration(storageFile);

        for (World world : capturePointClaims.claimWorlds)
            worldCache.put(world, HashBasedTable.create());

        //Load and cache data into worldCache from flatfile storage. Also purge unused entries
        for (World world : worldCache.keySet())
        {
            Set<String> keysToDelete = new HashSet<>();

            ConfigurationSection worldSection = regionStorage.getConfigurationSection(world.getName());
            for (String regionKey : worldSection.getKeys(false))
            {
                ConfigurationSection regionSection = regionStorage.getConfigurationSection(world.getName()).getConfigurationSection(regionKey);
                if (regionSection.getString("clanTag") == null)
                {
                    keysToDelete.add(regionKey);
                    continue;
                }
                //Cache region
                String[] values = regionKey.split(",");

                try
                {
                    getRegion(world, Integer.parseInt(values[0]), Integer.parseInt(values[1]), false);
                }
                catch (NumberFormatException e)
                {
                    keysToDelete.add(regionKey);
                    System.out.println("[CapturePointClaims] failed to load a region into cache, deleting it. " + e.getMessage());
                }
            }

            for (String deleteKey : keysToDelete)
            {
                worldSection.set(deleteKey, null);
            }
        }

        new BukkitRunnable()
        {
            public void run()
            {
                saveData(capturePointClaims);
            }
        }.runTaskTimer(capturePointClaims, 6000L, 6000L);
    }

    public void saveData(CapturePointClaims capturePointClaims)
    {
        File storageFile = new File(capturePointClaims.getDataFolder(), "regionStorage.data");
        if (regionStorage != null)
        {
            try
            {
                regionStorage.save(storageFile);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    //given a location, returns the coordinates of the region containing that location
    //returns NULL when the location is not in the managed world
    public Region getRegion(Location location)
    {
        if (!worldCache.containsKey(location.getWorld()))
            return null;
        //keeping all regions the same size and arranging them in a strict grid makes this calculation supa-fast! //ses da bigscary
        int x = location.getBlockX() / REGION_SIZE;
        if(location.getX() < 0) x--;

        int z = location.getBlockZ() / REGION_SIZE;
        if(location.getZ() < 0) z--;

        Region region = worldCache.get(location.getWorld()).get(x, z); //Get the cached Region object
        if (region == null) //create a new one if such doesn't exist
        {
            worldCache.get(location.getWorld()).put(x, z, new Region(x, z, location.getWorld(), REGION_SIZE, regionStorage));
            region = worldCache.get(location.getWorld()).get(x, z);
        }
        return region;
    }

    public Region getRegion(World world, int x, int z)
    {
        return getRegion(world, x, z, true);
    }

    public Region getRegion(World world, int x, int z, boolean cacheOnly)
    {
        if (!worldCache.containsKey(world))
            return null;
        Region region = worldCache.get(world).get(x, z); //Get the cached Region object
        if (!cacheOnly && region == null) //create a new one if such doesn't exist
        {
            worldCache.get(world).put(x, z, new Region(x, z, world, REGION_SIZE, regionStorage));
            region = worldCache.get(world).get(x, z);
        }
        return region;
    }

    public Set<Region> getRegions(String clanTag)
    {
        Set<Region> regionsToReturn = new HashSet<>();
        for (Table<Integer, Integer, Region> world : worldCache.values())
        {
            for (Region region : world.values())
            {
                if (region.getOwningClanTag() != null && region.getOwningClanTag().equals(clanTag))
                    regionsToReturn.add(region);
            }
        }
        return regionsToReturn;
    }

}

