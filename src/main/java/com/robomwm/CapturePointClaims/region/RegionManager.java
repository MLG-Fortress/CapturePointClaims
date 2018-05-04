package com.robomwm.CapturePointClaims.region;

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
import com.robomwm.CapturePointClaims.CapturePointClaims;
import com.robomwm.grandioseapi.GrandioseAPI;
import com.robomwm.grandioseapi.player.GrandPlayerManager;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RegionManager
{
    private CapturePointClaims instance;
    private final int REGION_SIZE = 400;
    private YamlConfiguration regionStorage;
    private Map<World, Table<Integer, Integer, Region>> worldCache = new HashMap<>();
    private GrandPlayerManager grandPlayerManager;

    public GrandPlayerManager getGrandPlayerManager()
    {
        return grandPlayerManager;
    }

    public RegionManager(CapturePointClaims plugin)
    {
        instance = plugin;
        grandPlayerManager = ((GrandioseAPI)plugin.getServer().getPluginManager().getPlugin("GrandioseAPI")).getGrandPlayerManager();
        File storageFile = new File(plugin.getDataFolder(), "regionStorage.data");
        if (!storageFile.exists())
        {
            try
            {
                storageFile.createNewFile();
            }
            catch (IOException e)
            {
                plugin.getLogger().severe("Could not create regionStorage.data! Since I'm lazy, there currently is no \"in memory\" option. Will now disable along with a nice stack trace for you to bother me with:");
                e.printStackTrace();
                return;
            }
        }
        regionStorage = YamlConfiguration.loadConfiguration(storageFile);

        for (World world : plugin.claimWorlds)
            worldCache.put(world, HashBasedTable.create());

        //Load and cache data into worldCache from flatfile storage. Also purge unused entries
        for (World world : worldCache.keySet())
        {
            Set<String> keysToDelete = new HashSet<>();

            ConfigurationSection worldSection = regionStorage.getConfigurationSection(world.getName());
            if (worldSection == null)
                continue;
            for (String regionKey : worldSection.getKeys(false))
            {
//                ConfigurationSection regionSection = regionStorage.getConfigurationSection(world.getName()).getConfigurationSection(regionKey);
//                if (regionSection.getString("clanTag") == null || plugin.getClanManager().getClan(regionSection.getString("clanTag")) == null)
//                {
//                    keysToDelete.add(regionKey);
//                    continue;
//                }
                //Cache region
                String[] values = regionKey.split(",");

                try
                {
                    getRegion(world, Integer.parseInt(values[0]), Integer.parseInt(values[1]), false);
                }
                catch (NumberFormatException e)
                {
                    keysToDelete.add(regionKey);
                    plugin.getLogger().warning("Failed to load a region into cache, deleting it. " + e.getMessage());
                }
            }

            for (String deleteKey : keysToDelete)
            {
                worldSection.set(deleteKey, null);
            }
        }
    }

    //given a location, returns the coordinates of the region containing that location
    //returns NULL when the location is not in the managed world
    public Region getRegion(Location location)
    {
        if (!worldCache.containsKey(location.getWorld()))
            return null;
        int x = location.getBlockX() / REGION_SIZE;
        if(location.getX() < 0) x--;

        int z = location.getBlockZ() / REGION_SIZE;
        if(location.getZ() < 0) z--;

        Region region = worldCache.get(location.getWorld()).get(x, z); //Get the cached Region object
        if (region == null) //create a new one if such doesn't exist
            region = loadRegion(location.getWorld(), x, z);
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
            region = loadRegion(world, x, z);
        return region;
    }

    public Set<Region> getRegions(Clan clan)
    {
        Set<Region> regionsToReturn = new HashSet<>();
        for (Table<Integer, Integer, Region> world : worldCache.values())
        {
            for (Region region : world.values())
            {
                if (region.getOwner() != null
                        && instance.getClanManager().getClanByPlayerUniqueId(region.getOwner().getUniqueId()) == clan)
                    regionsToReturn.add(region);
            }
        }
        return regionsToReturn;
    }

    public Set<Region> getRegions(Player player)
    {
        Set<Region> regionsToReturn = new HashSet<>();
        for (Table<Integer, Integer, Region> world : worldCache.values())
        {
            for (Region region : world.values())
            {
                if (region.getOwner() != null && region.getOwner().getPlayer() == player)
                    regionsToReturn.add(region);
            }
        }
        return regionsToReturn;
    }

    public Set<Region> getRegions()
    {
        Set<Region> regions = new HashSet<>();
        for (World world : worldCache.keySet())
        {
            regions.addAll(worldCache.get(world).values());
        }
        return regions;
    }

    private Region loadRegion(World world, int x, int z)
    {
        if (!worldCache.containsKey(world))
            return null;

        Region region = new Region(x, z, world, REGION_SIZE, this);

        ConfigurationSection worldSection = regionStorage.getConfigurationSection(region.getWorld().getName());
        if (worldSection != null)
        {
            ConfigurationSection regionSection = worldSection.getConfigurationSection(region.toString());
            if (regionSection != null)
            {
                //If it's empty, delete it
                if (regionSection.getKeys(false).isEmpty())
                    worldSection.set(region.toString(), null);

                region.setHealth(regionSection.getInt("health", 100));
                region.addFuel(regionSection.getInt("fuel"));
                region.setArrows(regionSection.getInt("arrows"));
                region.setGolems(regionSection.getInt("golems"));
                region.setZerg(regionSection.getInt("zerg"));
                if (regionSection.contains("owner"))
                    region.setOwner(instance.getServer().getOfflinePlayer(UUID.fromString(regionSection.getString("owner"))));
                //saveRegion(region); //Used for storage upgrade conversion, uncomment when needed.
            }
        }
        worldCache.get(world).put(x, z, region);
        return worldCache.get(world).get(x, z);
    }

    public boolean saveRegion(Region region)
    {
        if (!worldCache.containsKey(region.getWorld()))
            return false;
        ConfigurationSection worldSection = regionStorage.getConfigurationSection(region.getWorld().getName());
        if (worldSection == null)
            worldSection = regionStorage.createSection(region.getWorld().getName());
        ConfigurationSection regionSection = worldSection.getConfigurationSection(region.toString());
        if (regionSection == null)
            regionSection = worldSection.createSection(region.toString());

        //Delete region from storage if it's unclaimed
        if (region.getOwner() == null)
            worldSection.set(region.toString(), null);
        else
        {
            //old values we no longer use
            regionSection.set("clanColor", null);
            regionSection.set("captureTime", null);
            //stuff we do use
            regionSection.set("owner", region.getOwner().getUniqueId().toString());
            regionSection.set("health", region.getHealth());
            regionSection.set("fuel", region.getFuel());
            regionSection.set("arrows", region.getArrows());
            regionSection.set("golems", region.getGolems());
            regionSection.set("zerg", region.getZerg());
        }
        File storageFile = new File(instance.getDataFolder(), "regionStorage.data");
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
        return true;
    }

    public boolean isEnemyClaim(Region region, Player player, boolean includeWildernessAsEnemy)
    {
        if (region.getOwner() == null)
            return includeWildernessAsEnemy;
        if (player.getUniqueId().equals(region.getOwner().getUniqueId()))
            return false;
        return isEnemyClan(player, region.getOwner(), includeWildernessAsEnemy);
    }

    public boolean isEnemyClaim(Location targetLocation, Player player, boolean includeWildernessAsEnemy)
    {
        if (!instance.claimWorlds.contains(targetLocation.getWorld()))
            return false;
        return isEnemyClaim(getRegion(targetLocation), player, includeWildernessAsEnemy);
    }

    public boolean isEnemyClan(Player player, OfflinePlayer owner, boolean includeWildernessAsEnemy)
    {
        if (owner == null) //Unclaimed
            return includeWildernessAsEnemy;

        Clan ownerClan = instance.getClanManager().getClanByPlayerUniqueId(owner.getUniqueId());
        Clan playerClan = instance.getClanManager().getClanByPlayerUniqueId(player.getUniqueId());
        return ownerClan == null || playerClan == null || playerClan != ownerClan && !playerClan.isAlly(ownerClan.getTag());
    }

}

