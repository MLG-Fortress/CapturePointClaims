package to.us.tf.CapturePointClaims;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import to.us.tf.CapturePointClaims.managers.RegionManager;

import javax.annotation.Nonnull;

public class Region
{
    private int regionX;
    private int regionZ;
    private World world;
    private Clan owningClan;
    private int REGION_SIZE;
    private int health = 50;
    private int captureTime = 15;
    private RegionManager regionManager;

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

    public Clan getClan() //Must be converted to a clan
    {
        return this.owningClan;
    }

    public void setOwningClanTag(Clan clan)
    {
        this.owningClan = clan;
    }

    public int getHealth()
    {
        return health;
    }

    public void setHealth(int health)
    {
        this.health = health;
    }

    public int getCaptureTime()
    {
        return captureTime;
    }

    public void setCaptureTime(int captureTime)
    {
        this.captureTime = captureTime;
    }

    private byte getClanColorValue()
    {
        if (owningClan == null)
            return DyeColor.WHITE.getWoolData();

        char clanTagChar = owningClan.getColorTag().charAt(1);
        ChatColor chatColor = ChatColor.getByChar(clanTagChar);
        DyeColor dyeColor = DyeColor.WHITE;

        switch (chatColor)
        {
            case LIGHT_PURPLE:
            case DARK_PURPLE:
                dyeColor = DyeColor.PURPLE;
                break;
            case GOLD:
                dyeColor = DyeColor.YELLOW;
                break;
            case DARK_GREEN:
            case GREEN:
                dyeColor = DyeColor.GREEN;
                break;
            case AQUA:
                dyeColor = DyeColor.CYAN;
                break;
        }

        return dyeColor.getWoolData();
    }

    /**
     * Only way to "assign" a new clan to a region
     * @param clan
     * @param instance
     */
    public void changeOwner(Clan clan, CapturePointClaims instance)
    {
        this.setOwningClanTag(clan);
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
        glass.setType(Material.STAINED_GLASS);
        glass.setData(getClanColorValue());

        //build outer platform
        for(int x1 = x - 2; x1 <= x + 2; x1++)
        {
            for(int z1 = z - 2; z1 <= z + 2; z1++)
            {
                world.getBlockAt(x1, y, z1).setType(Material.BEDROCK);
            }
        }

        //build inner platform
        for(int x1 = x - 1; x1 <= x + 1; x1++)
        {
            for(int z1 = z - 1; z1 <= z + 1; z1++)
            {
                world.getBlockAt(x1, y, z1).setType(Material.BEDROCK);
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

                org.bukkit.material.Sign signData = new org.bukkit.material.Sign(Material.WALL_SIGN);
                signData.setFacingDirection(BlockFace.NORTH);

                block1.setTypeIdAndData(Material.WALL_SIGN.getId(), signData.getData(), false);

                Sign sign = (Sign)block1.getState();

                sign.setLine(0, world.getName() + " " + regionX + " " + regionZ);
                sign.setLine(2, "Break to capture");
                sign.setLine(3, "this point");

                sign.update();
            }
        }.runTaskLater(instance, 2L);
    }
}
