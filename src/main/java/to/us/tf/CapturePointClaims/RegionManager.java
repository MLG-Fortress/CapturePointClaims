package to.us.tf.CapturePointClaims;

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
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.scheduler.BukkitRunnable;

public class RegionManager
{
    final int REGION_SIZE = 400;

    Table<Integer, Integer, Region> cachedRegions = HashBasedTable.create();
    //given a location, returns the coordinates of the region containing that location
    //returns NULL when the location is not in the managed world
    //TRIVIA!  despite the simplicity of this method, I got it badly wrong like 5 times before it was finally fixed
    public Region fromLocation(Location location)
    {
        //keeping all regions the same size and arranging them in a strict grid makes this calculation supa-fast!
        //that's important because we do it A LOT as players move, build, break blocks, and more
        int x = location.getBlockX() / REGION_SIZE;
        if(location.getX() < 0) x--;

        int z = location.getBlockZ() / REGION_SIZE;
        if(location.getZ() < 0) z--;

        Region region = cachedRegions.get(x, z); //Get the cached Region object
        if (region == null) //create a new one if such doesn't exist
        {
            cachedRegions.put(x, z, new Region(x, z, location.getWorld(), REGION_SIZE));
            region = cachedRegions.get(x, z);
        }
        return region;
    }
}

class Region
{
    private int regionX;
    private int regionZ;
    private World world;
    private String owningClanTag; //Although clan names aren't mutable(?), there's only a method to get clans by tag.
    private int REGION_SIZE;
    private Byte clanColorValue;

    public Region(int regionX, int regionZ, World world, int regionSize)
    {
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.world = world;
        this.REGION_SIZE = regionSize;
    }

    //converts a string representing region coordinates to a proper region coordinates object
    //used in reading data from files and converting filenames themselves in some cases
    public Region(String string)
    {
        //split the input string on the space
        String [] elements = string.split(" ");

        //expect two elements - X and Z, respectively
        String xString = elements[0];
        String zString = elements[1];

        //convert those to integer values
        this.regionX = Integer.parseInt(xString);
        this.regionZ = Integer.parseInt(zString);
    }

    //opposite of above - converts region coordinates to a handy string
    public String toString()
    {
        return Integer.toString(this.regionX) + " " + Integer.toString(this.regionZ);
    }

    //compares two region coordinates to see if they match
    @Override
    public boolean equals(Object coordinatesToCompare)
    {
        if(coordinatesToCompare == null) return false;

        if(!(coordinatesToCompare instanceof RegionManager)) return false;

        Region coords = (Region)coordinatesToCompare;

        return this.regionX == coords.regionX && this.regionZ == coords.regionZ;
    }

    @Override
    public int hashCode()
    {
        return this.toString().hashCode();
    }

    public World getWorld()
    {
        return this.world;
    }

    public String getOwningClanTag() //Must be converted to a clan
    {
        if (this.owningClanTag == null)
        {
            //TODO: various data storage stuff to set owningClanTag
        }
        return this.owningClanTag;
    }

    private void setOwningClanTag(String clan)
    {
        this.owningClanTag = clan;
        //TODO: various data storage stuff
    }

    private void setClanColorValue(byte value)
    {
        this.clanColorValue = value;
    }

    private byte getClanColorValue()
    {
        if (this.clanColorValue == null)
        {
            //TODO: various data retrieval
        }

        if (this.clanColorValue == null)
            return DyeColor.WHITE.getDyeData();
        return this.clanColorValue;
    }

    public void changeOwner(Clan clan)
    {
        DyeColor dyeColor = DyeColor.WHITE;
        if (clan == null)
        {
            this.setOwningClanTag(null);
            this.setClanColorValue(dyeColor.getDyeData());
        }
        this.setOwningClanTag(clan.getTag());
        char clanTagChar = clan.getTag().charAt(1);
        switch (clanTagChar)
        {
            case 'a':
                dyeColor = DyeColor.GREEN;
                break;
            case 'b':
                dyeColor = DyeColor.LIGHT_BLUE;
                break;
            case 'd':
                dyeColor = DyeColor.PURPLE;
                break;
            case 'e':
                dyeColor = DyeColor.YELLOW;
                break;
            case 'c':
                dyeColor = DyeColor.RED;
                break;
        }
        this.setClanColorValue(dyeColor.getDyeData());
    }

    //determines the center of a region (as a Location) given its region coordinates
    //keeping all regions the same size and aligning them in a grid keeps this calculation simple and fast
    public Location getRegionCenter(boolean computeY)
    {
        int x1, z1;
        x1 = this.regionX * REGION_SIZE + REGION_SIZE / 2;
        z1 = this.regionZ * REGION_SIZE + REGION_SIZE / 2;

        Location center = new Location(world, x1, 62, z1);

        if(computeY) center = world.getHighestBlockAt(center).getLocation();

        return center;
    }

    public boolean nearRegionPost(Location location, int howClose)
    {
        Location postLocation = getRegionCenter(false);

        //NOTE!  Why not use distance?  Because I want a box to the sky, not a sphere.
        //Why not round?  Below calculation is cheaper than distance (needed for a cylinder or sphere).
        //Why to the sky?  Because if somebody builds a platform above the post, folks will teleport onto that platform by mistake.
        //Also...  lava from above would be bad.
        //Why not below?  Because I can't imagine mining beneath a post as an avenue for griefing.

        return (	location.getBlockX() >= postLocation.getBlockX() - howClose &&
                location.getBlockX() <= postLocation.getBlockX() + howClose &&
                location.getBlockZ() >= postLocation.getBlockZ() - howClose &&
                location.getBlockZ() <= postLocation.getBlockZ() + howClose &&
                location.getBlockY() >= location.getWorld().getHighestBlockYAt(postLocation) - 4
        );
    }

    //ensures a piece of the managed world is loaded into server memory
    //(generates the chunk if necessary)
    //these coordinate params are BLOCK coordinates, not CHUNK coordinates
    public static boolean GuaranteeChunkLoaded(Location location)
    {
        Chunk chunk = location.getWorld().getChunkAt(location);
        if(!chunk.isLoaded())
        {
            if(!chunk.load(true))
            {
                return false;
            }
        }
        return true;
    }

    //actually edits the world to create a region post at the center of the specified region
    @SuppressWarnings("deprecation")
    public void AddRegionPost(CapturePointClaims instance)
    {
        //find the center
        Location regionCenter = getRegionCenter(false);
        int x = regionCenter.getBlockX();
        int z = regionCenter.getBlockZ();
        int y;

        //make sure data is loaded for that area, because we're about to request data about specific blocks there
        if (!GuaranteeChunkLoaded(new Location(world, x, 5, z)))
            return;

        //sink lower until we find something solid
        //also ignore glowstone, in case there's already a post here!
        Material blockType;

        //find the highest block.  could be the surface, a tree, some grass...
        y = world.getHighestBlockYAt(x, z) + 1;

        //posts fall through trees, snow, and any existing post looking for the ground
        do
        {
            blockType = world.getBlockAt(x, --y, z).getType();
        }
        while(	y > 0 && (
                blockType == Material.AIR 		||
                        blockType == Material.LEAVES 	||
                        blockType == Material.LEAVES_2  ||
                        blockType == Material.LONG_GRASS||
                        blockType == Material.LOG       ||
                        blockType == Material.LOG_2     ||
                        blockType == Material.SNOW 		||
                        blockType == Material.VINE
        ));

        if(blockType == Material.SIGN_POST)
        {
            y -= 4;
        }
        else if(blockType == Material.BEACON || blockType == Material.BARRIER)
        {
            y -= 1;
        }
        else if(blockType == Material.BEDROCK)
        {
            y += 1;
        }

        //if y value is under sea level, correct it to sea level (no posts should be that difficult to find)
        if(y < 62)
        {
            y = 62;
        }

        //clear signs from the area, this ensures signs don't drop as items
        //when the blocks they're attached to are destroyed in the next step
        for(int x1 = x - 2; x1 <= x + 2; x1++)
        {
            for(int z1 = z - 2; z1 <= z + 2; z1++)
            {
                for(int y1 = y + 1; y1 <= y + 5; y1++)
                {
                    Block block = world.getBlockAt(x1, y1, z1);
                    if(block.getType() == Material.SIGN_POST || block.getType() == Material.SIGN || block.getType() == Material.WALL_SIGN)
                        block.setType(Material.AIR);
                }
            }
        }

        //clear above it - sometimes this shears trees in half (doh!)
        for(int x1 = x - 2; x1 <= x + 2; x1++)
        {
            for(int z1 = z - 2; z1 <= z + 2; z1++)
            {
                for(int y1 = y + 1; y1 < y + 5; y1++)
                {
                    Block block = world.getBlockAt(x1, y1, z1);
                    if(block.getType() != Material.AIR) block.setType(Material.AIR);
                }
            }
        }

        //Sometimes we don't clear high enough thanks to new ultra tall trees in jungle biomes
        //Instead of attempting to clear up to nearly 110 * 4 blocks more, we'll just see what getHighestBlockYAt returns
        //If it doesn't return our post's y location, we're setting it and all blocks below to air.
        int highestBlockY = world.getHighestBlockYAt(x, z);
        while (highestBlockY > y)
        {
            Block block = world.getBlockAt(x, highestBlockY, z);
            if(block.getType() != Material.AIR)
                block.setType(Material.BARRIER);
            highestBlockY--;
        }

        //build top block
        world.getBlockAt(x, y + 3, z).setType(Material.BARRIER);
        Block glass = world.getBlockAt(x, y + 2, z);
        glass.setType(Material.STAINED_GLASS);
        glass.setData(getClanColorValue());

        //build outer platform
        for(int x1 = x - 2; x1 <= x + 2; x1++)
        {
            for(int z1 = z - 2; z1 <= z + 2; z1++)
            {
                world.getBlockAt(x1, y, z1).setType(Material.EMERALD_BLOCK);
            }
        }

        //build inner platform
        for(int x1 = x - 1; x1 <= x + 1; x1++)
        {
            for(int z1 = z - 1; z1 <= z + 1; z1++)
            {
                world.getBlockAt(x1, y, z1).setType(Material.EMERALD_BLOCK);
            }
        }

        //build lower center blocks
        int y1 = y;
        world.getBlockAt(x, y1, z).setType(Material.EMERALD_BLOCK);
        world.getBlockAt(x, ++y1, z).setType(Material.BEACON);

        //build a sign on top with region name (or wilderness if no name)
//        String regionName = region.getOwningClanTag();
//        if(regionName == null) regionName = "Nobody";
//        Block block = world.getBlockAt(regionX, y + 4, regionZ);
//        block.setType(Material.SIGN_POST);

        //final String finalRegionName = regionName;
        //final Block finalBlock = block;
        final int finalY = y;
        new BukkitRunnable()
        {
            //Block block1 = finalBlock;
            public void run()
            {
//                org.bukkit.block.Sign sign = (org.bukkit.block.Sign)block1.getState(); //Reason why we're making this a task
//                sign.setLine(1, "Owned by");
//                sign.setLine(2, finalRegionName);
//                sign.update();

                //custom signs

                Block block1 = world.getBlockAt(x, finalY + 3, z - 1);

                org.bukkit.material.Sign signData = new org.bukkit.material.Sign(Material.WALL_SIGN);
                signData.setFacingDirection(BlockFace.NORTH);

                block1.setTypeIdAndData(Material.WALL_SIGN.getId(), signData.getData(), false);

                Sign sign = (org.bukkit.block.Sign)block1.getState();

                sign.setLine(0, "Break this");
                sign.setLine(1, "to start");
                sign.setLine(2, "capture process");

                sign.update();
            }
        }.runTaskLater(instance, 2L);
    }
}

