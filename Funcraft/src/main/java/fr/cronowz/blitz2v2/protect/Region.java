package fr.cronowz.blitz2v2.protect;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class Region {
    private final String id;
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public Region(String id, String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.id = id;
        this.worldName = worldName;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public String getId() { return id; }
    public String getWorldName() { return worldName; }

    public boolean contains(Block b) {
        if (b == null) return false;
        return contains(b.getWorld(), b.getX(), b.getY(), b.getZ());
    }

    public boolean contains(Location loc) {
        if (loc == null) return false;
        return contains(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private boolean contains(World w, int x, int y, int z) {
        if (w == null) return false;
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
}
