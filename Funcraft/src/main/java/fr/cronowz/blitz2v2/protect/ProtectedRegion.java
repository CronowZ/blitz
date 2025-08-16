// src/main/java/fr/cronowz/blitz2v2/protect/ProtectedRegion.java
package fr.cronowz.blitz2v2.protect;

import org.bukkit.Location;
import org.bukkit.World;

public class ProtectedRegion {
    private final String id;
    private final String baseWorld; // ex: "world_game"
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public ProtectedRegion(String id, String baseWorld, Location a, Location b) {
        this.id = id;
        this.baseWorld = baseWorld;
        this.minX = Math.min(a.getBlockX(), b.getBlockX());
        this.minY = Math.min(a.getBlockY(), b.getBlockY());
        this.minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        this.maxX = Math.max(a.getBlockX(), b.getBlockX());
        this.maxY = Math.max(a.getBlockY(), b.getBlockY());
        this.maxZ = Math.max(a.getBlockZ(), b.getBlockZ());
    }

    public ProtectedRegion(String id, String baseWorld,
                           int minX, int minY, int minZ,
                           int maxX, int maxY, int maxZ) {
        this.id = id;
        this.baseWorld = baseWorld;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public String getId() { return id; }
    public String getBaseWorld() { return baseWorld; }

    /** Renvoie true si loc est dans ce cuboïde et si le monde correspond à la "famille" du baseWorld. */
    public boolean contains(Location loc, ProtectionManager pm) {
        World w = loc.getWorld();
        if (w == null) return false;
        String name = w.getName();
        if (!pm.isWorldInFamily(name, baseWorld)) return false;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return (x >= minX && x <= maxX) &&
                (y >= minY && y <= maxY) &&
                (z >= minZ && z <= maxZ);
    }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
}
