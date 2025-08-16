package fr.cronowz.blitz2v2.protect;

import fr.cronowz.blitz2v2.Blitz2v2;
import org.bukkit.BlockChangeDelegate;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RegionManager {
    private final Blitz2v2 plugin;
    private final Map<String, Region> regions = new LinkedHashMap<>();
    private final File file;
    private YamlConfiguration yaml;

    public RegionManager(Blitz2v2 plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "regions.yml");
        load();
    }

    public Collection<Region> getRegions() {
        return Collections.unmodifiableCollection(regions.values());
    }

    public Region getRegion(String id) {
        return regions.get(id.toLowerCase());
    }

    public boolean exists(String id) {
        return regions.containsKey(id.toLowerCase());
    }

    public Region createRegion(String id, Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) return null;
        String worldName = a.getWorld().getName();

        Region r = new Region(
                id,
                worldName,
                a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                b.getBlockX(), b.getBlockY(), b.getBlockZ()
        );
        regions.put(id.toLowerCase(), r);
        save();
        return r;
    }

    public boolean remove(String id) {
        Region removed = regions.remove(id.toLowerCase());
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    /** IMPORTANT : applique les régions d'un clone de monde de jeu à tous les autres clones. */
    private boolean worldMatches(String regionWorldName, World currentWorld) {
        if (regionWorldName == null || currentWorld == null) return false;

        String current = currentWorld.getName();
        if (regionWorldName.equalsIgnoreCase(current)) {
            return true; // match exact
        }

        String base = Blitz2v2.getInstance().getGameWorldName(); // ex: "world_game"
        boolean regionIsGameClone  = regionWorldName.equalsIgnoreCase(base) || regionWorldName.startsWith(base + "_");
        boolean currentIsGameClone = current.equalsIgnoreCase(base)        || current.startsWith(base + "_");
        return regionIsGameClone && currentIsGameClone;
    }

    public boolean isProtected(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        for (Region r : regions.values()) {
            if (worldMatches(r.getWorldName(), loc.getWorld()) && r.contains(loc)) {
                return true;
            }
        }
        return false;
    }

    // ───── persistence ─────

    public void load() {
        regions.clear();
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer regions.yml : " + e.getMessage());
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.isConfigurationSection("regions")) return;

        for (String id : yaml.getConfigurationSection("regions").getKeys(false)) {
            String path = "regions." + id + ".";
            String world = yaml.getString(path + "world");
            int minX = yaml.getInt(path + "minX");
            int minY = yaml.getInt(path + "minY");
            int minZ = yaml.getInt(path + "minZ");
            int maxX = yaml.getInt(path + "maxX");
            int maxY = yaml.getInt(path + "maxY");
            int maxZ = yaml.getInt(path + "maxZ");
            Region r = new Region(id, world, minX, minY, minZ, maxX, maxY, maxZ);
            regions.put(id.toLowerCase(), r);
        }
        plugin.getLogger().info("Regions chargées: " + regions.size());
    }

    public void save() {
        if (yaml == null) yaml = new YamlConfiguration();
        yaml.set("regions", null);
        for (Region r : regions.values()) {
            String path = "regions." + r.getId() + ".";
            yaml.set(path + "world", r.getWorldName());
            yaml.set(path + "minX", r.getMinX());
            yaml.set(path + "minY", r.getMinY());
            yaml.set(path + "minZ", r.getMinZ());
            yaml.set(path + "maxX", r.getMaxX());
            yaml.set(path + "maxY", r.getMaxY());
            yaml.set(path + "maxZ", r.getMaxZ());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur sauvegarde regions.yml : " + e.getMessage());
        }
    }
}
