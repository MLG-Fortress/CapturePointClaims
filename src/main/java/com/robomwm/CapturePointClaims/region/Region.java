package com.robomwm.CapturePointClaims.region;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Region
{
    private int regionX;
    private int regionZ;
    private World world;
    private OfflinePlayer owner;
    private int REGION_SIZE;
    private int health = 50;
    private int fuel;
    private long timeCaptured;
    private RegionManager regionManager;

    public void setTimeCaptured(long timeCaptured)
    {
        this.timeCaptured = timeCaptured;
    }

    public long getTimeCaptured()
    {
        return timeCaptured;
    }

    public String getName()
    {
        return getWorld().getName() + " " + regionX + " " + regionZ;
    }

    public Region(int regionX, int regionZ, World world, int regionSize, RegionManager regionManager)
    {
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.world = world;
        this.REGION_SIZE = regionSize;
        this.regionManager = regionManager;
    }

    public RegionManager getRegionManager()
    {
        return regionManager;
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
        return Integer.toString(this.regionX) + "," + Integer.toString(this.regionZ);
    }

    //compares two region coordinates to see if they match
    @Override
    public boolean equals(Object coordinatesToCompare)
    {
        if(coordinatesToCompare == null) return false;

        if(!(coordinatesToCompare instanceof Region)) return false;

        Region coords = (Region)coordinatesToCompare;

        return this.world == coords.world && this.regionX == coords.regionX && this.regionZ == coords.regionZ;
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

    public OfflinePlayer getOwner() //Must be converted to a clan
    {
        return this.owner;
    }

    public void setOwner(OfflinePlayer player)
    {
        this.owner = player;
    }

    public int getHealth()
    {
        return health;
    }

    public void setHealth(int health)
    {
        this.health = health;
    }

    public void addHealth(int health)
    {
        this.health += health;
    }

    public int getFuel()
    {
        return fuel;
    }

    public void addFuel(int fuel)
    {
        this.fuel += fuel;
        if (fuel < 0)
            this.fuel = 0;
    }

    public void setFuel(int fuel)
    {
        this.fuel = fuel;
    }

    private Material getStainedGlassColor()
    {
        if (owner == null)
            return Material.WHITE_STAINED_GLASS;

        ChatColor chatColor = regionManager.getGrandPlayerManager().getGrandPlayer(owner).getNameColor();

        switch (chatColor) //TODO: unfinished
        {
            case LIGHT_PURPLE:
                return Material.MAGENTA_STAINED_GLASS;
            case DARK_PURPLE:
                return Material.PURPLE_STAINED_GLASS;
            case YELLOW:
                return Material.YELLOW_STAINED_GLASS;
            case GOLD:
                return Material.ORANGE_STAINED_GLASS;
            case DARK_GREEN:
                return Material.GREEN_STAINED_GLASS;
            case GREEN:
                return Material.LIME_STAINED_GLASS;
            case AQUA:
                return Material.LIGHT_BLUE_STAINED_GLASS;
            case BLUE:
                return Material.BLUE_STAINED_GLASS;
        }

        return Material.BROWN_STAINED_GLASS;
    }

    /**
     * Only way to "assign" a new clan to a region
     * @param player
     * @param instance
     */
    public void changeOwner(OfflinePlayer player, JavaPlugin instance)
    {
        this.setOwner(player);
        health = 100;
        fuel = 30;
        this.AddRegionPost(instance);
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

        //NOTE!  Why not use distance?  Because I want a box, not a sphere.
        //Why not round?  Below calculation is cheaper than distance (needed for a cylinder or sphere).
        //Also...  lava from above would be bad.
        //Why not below?  Because I can't imagine mining beneath a post as an avenue for griefing.

        return (	location.getBlockX() >= postLocation.getBlockX() - howClose &&
                location.getBlockX() <= postLocation.getBlockX() + howClose &&
                location.getBlockZ() >= postLocation.getBlockZ() - howClose &&
                location.getBlockZ() <= postLocation.getBlockZ() + howClose &&
                location.getBlockY() >= location.getWorld().getHighestBlockYAt(postLocation) - 4 &&
                location.getBlockY() <= location.getWorld().getHighestBlockYAt(postLocation) + 8
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
    public void AddRegionPost(JavaPlugin instance)
    {
        //find the center
        Location regionCenter = getRegionCenter(false);
        int x = regionCenter.getBlockX();
        int z = regionCenter.getBlockZ();
        int y;

        //make sure data is loaded for that area, because we're about to request data about specific blocks there
        if (!(new Location(world, x, 5, z)).getChunk().isLoaded())
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
                        Tag.LEAVES.isTagged(blockType)	||
                        blockType == Material.GRASS     ||
                        blockType == Material.TALL_GRASS||
                        Tag.LOGS.isTagged(blockType)    ||
                        blockType == Material.SNOW 		||
                        blockType == Material.VINE      ||
                        blockType.getHardness() == 0f
        ));

        if(blockType == Material.SIGN)
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
        else if(blockType == Material.AIR) //void, don't build
        {
            return;
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
                    if(block.getType() == Material.SIGN || block.getType() == Material.WALL_SIGN)
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
        int highestBlockY = 256;
        while (highestBlockY > y)
        {
            Block block = world.getBlockAt(x, highestBlockY, z);
            if(block.getType() != Material.BARRIER)
                block.setType(Material.BARRIER);
            highestBlockY--;
        }

        //build top block
        world.getBlockAt(x, y + 3, z).setType(Material.BARRIER);
        Block glass = world.getBlockAt(x, y + 2, z);
        glass.setType(getStainedGlassColor());

        //build outer platform
        for(int x1 = x - 2; x1 <= x + 2; x1++)
        {
            for(int z1 = z - 2; z1 <= z + 2; z1++)
            {
                world.getBlockAt(x1, y, z1).setType(Material.COMMAND_BLOCK);
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
//        String regionName = region.getClan();
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

                block1.setType(Material.WALL_SIGN);

                WallSign signBlockData = (WallSign)block1.getBlockData();
                signBlockData.setFacing(BlockFace.NORTH);
                block1.setBlockData(signBlockData);

                Sign sign = (Sign)block1.getState();
                sign.setLine(0, world.getName() + " " + regionX + " " + regionZ);
                if (owner != null)
                    sign.setLine(1, owner.getName());
                sign.setLine(2, "Break to capture");
                sign.setLine(3, "this post");

                sign.update();
            }
        }.runTaskLater(instance, 2L);
    }
}
