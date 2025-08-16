// src/main/java/fr/cronowz/blitz2v2/protect/ProtectionManager.java
package fr.cronowz.blitz2v2.protect;

import fr.cronowz.blitz2v2.Blitz2v2;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ProtectionManager {

    private final Blitz2v2 plugin;

    /** Bases des mondes de jeu à protéger automatiquement (ex: ["world_game"]) */
    private final Set<String> gameBases = new HashSet<>();
    /** Coffres incassables : bases de mondes concernées (ex: ["world_game","world_waiting","world_lobby"]) */
    private final Set<String> chestBases = new HashSet<>();
    private boolean chestsUnbreakable = true;

    /** Régions par base (clé = baseWorld) */
    private final Map<String, List<ProtectedRegion>> regions = new HashMap<>();

    /** Sélections par joueur */
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public ProtectionManager(Blitz2v2 plugin) {
        this.plugin = plugin;
    }

    /* ===================== SÉLECTION (wand) ===================== */

    public void setPos1(UUID playerId, Location loc) {
        if (playerId == null || loc == null) return;
        pos1.put(playerId, loc.clone());
    }

    public void setPos2(UUID playerId, Location loc) {
        if (playerId == null || loc == null) return;
        pos2.put(playerId, loc.clone());
    }

    public Location getPos1(UUID playerId) {
        Location l = pos1.get(playerId);
        return l == null ? null : l.clone();
    }

    public Location getPos2(UUID playerId) {
        Location l = pos2.get(playerId);
        return l == null ? null : l.clone();
    }

    public boolean hasFullSelection(UUID playerId) {
        return pos1.containsKey(playerId) && pos2.containsKey(playerId);
    }

    public void clearSelection(UUID playerId) {
        pos1.remove(playerId);
        pos2.remove(playerId);
    }

    /**
     * Construit et enregistre une région à partir de la sélection du joueur.
     * @param playerId UUID du joueur
     * @param regionId identifiant (libre) de la région
     * @param worldName monde où la sélection a été faite (on en déduira la base)
     * @return la région créée, ou null si sélection incomplète
     */
    public ProtectedRegion createRegionFromSelection(UUID playerId, String regionId, String worldName) {
        Location a = getPos1(playerId);
        Location b = getPos2(playerId);
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) return null;

        // On autorise worldName passé en param, sinon on prend celui de a
        String base = resolveBaseForWorld(worldName != null ? worldName : a.getWorld().getName());

        int minX = Math.min(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        int maxX = Math.max(a.getBlockX(), b.getBlockX());
        int maxY = Math.max(a.getBlockY(), b.getBlockY());
        int maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        ProtectedRegion r = new ProtectedRegion(regionId, base, minX, minY, minZ, maxX, maxY, maxZ);
        addRegion(r);
        clearSelection(playerId);
        return r;
    }

    /* ===================== CHARGEMENT / SAUVEGARDE ===================== */

    /** Recharge tout depuis config.yml */
    public void reloadFromConfig() {
        gameBases.clear();
        chestBases.clear();
        regions.clear();

        ConfigurationSection prot = plugin.getCfg().getConfigurationSection("protection");
        if (prot == null) return;

        chestsUnbreakable = prot.getBoolean("chests.unbreakable", true);
        List<String> cb = prot.getStringList("chests.world_bases");
        if (cb != null) chestBases.addAll(cb);

        List<String> gb = prot.getStringList("game_bases");
        if (gb != null) gameBases.addAll(gb);

        // régions
        List<Map<?, ?>> list = prot.getMapList("regions");
        for (Map<?, ?> m : list) {
            try {
                String id   = String.valueOf(m.get("id"));
                String base = String.valueOf(m.get("base"));
                Map<?, ?> p1 = (Map<?, ?>) m.get("pos1");
                Map<?, ?> p2 = (Map<?, ?>) m.get("pos2");

                int x1 = ((Number) p1.get("x")).intValue();
                int y1 = ((Number) p1.get("y")).intValue();
                int z1 = ((Number) p1.get("z")).intValue();
                int x2 = ((Number) p2.get("x")).intValue();
                int y2 = ((Number) p2.get("y")).intValue();
                int z2 = ((Number) p2.get("z")).intValue();

                ProtectedRegion r = new ProtectedRegion(id, base, x1, y1, z1, x2, y2, z2);
                regions.computeIfAbsent(base, k -> new ArrayList<>()).add(r);
            } catch (Exception ignore) { }
        }
    }

    /** Ajoute une région et la sauve en config. */
    @SuppressWarnings("unchecked")
    public void addRegion(ProtectedRegion r) {
        regions.computeIfAbsent(r.getBaseWorld(), k -> new ArrayList<>()).add(r);

        // Sauvegarde dans config
        ConfigurationSection prot = plugin.getConfig().getConfigurationSection("protection");
        if (prot == null) prot = plugin.getConfig().createSection("protection");

        List<Map<String, Object>> list = (List<Map<String, Object>>) prot.getList("regions", new ArrayList<>());
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", r.getId());
        entry.put("base", r.getBaseWorld());
        Map<String, Object> p1 = new LinkedHashMap<>();
        Map<String, Object> p2 = new LinkedHashMap<>();
        p1.put("x", r.getMinX()); p1.put("y", r.getMinY()); p1.put("z", r.getMinZ());
        p2.put("x", r.getMaxX()); p2.put("y", r.getMaxY()); p2.put("z", r.getMaxZ());
        entry.put("pos1", p1);
        entry.put("pos2", p2);
        list.add(entry);
        prot.set("regions", list);
        plugin.saveConfig();
    }

    /* ===================== REQUÊTES ===================== */

    public boolean isProtected(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        String world = loc.getWorld().getName();

        for (String base : gameBases) {
            if (isWorldInFamily(world, base)) {
                List<ProtectedRegion> list = regions.get(base);
                if (list == null) continue;
                for (ProtectedRegion r : list) {
                    if (r.contains(loc, this)) return true;
                }
            }
        }
        return false;
    }

    public boolean isChestsUnbreakable() {
        return chestsUnbreakable;
    }

    /** True si le monde appartient à une base listée pour coffres incassables. */
    public boolean isChestProtectedWorld(String worldName) {
        for (String base : chestBases) {
            if (isWorldInFamily(worldName, base)) return true;
        }
        return false;
    }

    /** Détermine si un monde X appartient à la “famille” d’une base, ex: world_game, world_game_1, world_game-2… */
    public boolean isWorldInFamily(String worldName, String base) {
        if (worldName.equalsIgnoreCase(base)) return true;
        String wl = worldName.toLowerCase();
        String bl = base.toLowerCase();
        return wl.startsWith(bl + "_") || wl.startsWith(bl + "-");
    }

    /** Trouve la base applicable pour un monde donné (celle qui “matche” le prefixe), sinon renvoie le nom du monde. */
    public String resolveBaseForWorld(String worldName) {
        for (String base : gameBases) {
            if (isWorldInFamily(worldName, base)) return base;
        }
        return worldName; // fallback: base = monde exact
    }

    /* ===================== UTILITAIRES (optionnels) ===================== */

    /** Retourne une vue (copie) des régions pour debug/GUI éventuels */
    public Map<String, List<ProtectedRegion>> getAllRegionsSnapshot() {
        return regions.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> new ArrayList<>(e.getValue())));
    }
}
