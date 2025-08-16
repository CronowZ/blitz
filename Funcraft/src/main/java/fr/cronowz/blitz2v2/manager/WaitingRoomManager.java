// src/main/java/fr/cronowz/blitz2v2/manager/WaitingRoomManager.java
package fr.cronowz.blitz2v2.manager;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.listeners.TeamSelectListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.ScoreboardManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WaitingRoomManager {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static final Map<Integer, WaitingRoom> rooms = new LinkedHashMap<>();

    /** Auto-join : cherche la salle non full la plus remplie, sinon en crée une si on n’a pas atteint la limite. */
    public static boolean autoJoin(Player p) {
        FileConfiguration cfg = Blitz2v2.getInstance().getCfg();
        int maxRooms = cfg.getInt("waiting.max-rooms", 5);

        WaitingRoom best = rooms.values().stream()
                .filter(w -> !w.isFull() && !w.isStarted())
                .max(Comparator.comparingInt(w -> w.getParticipants().size()))
                .orElse(null);

        if (best == null && rooms.size() < maxRooms) {
            best = createRoom();
        }
        return best != null && best.addPlayer(p);
    }

    /** Retourne la première salle existante, ou en crée une si aucune. */
    public static WaitingRoom getOrCreateRoom() {
        if (rooms.isEmpty()) return createRoom();
        return rooms.values().iterator().next();
    }

    /** Permet de rejoindre une salle par son ID. */
    public static boolean joinRoom(int id, Player p) {
        WaitingRoom wr = rooms.get(id);
        return wr != null && wr.addPlayer(p);
    }

    /** Liste toutes les waiting-rooms. */
    public static List<WaitingRoom> getRooms() {
        return new ArrayList<>(rooms.values());
    }

    /** Supprime une salle et son monde associé (déchargement strict + suppression dossier). */
    public static void removeRoom(int id) {
        WaitingRoom wr = rooms.remove(id);
        if (wr != null) {
            String name = wr.getSpawn().getWorld().getName();
            unloadAndDeleteStrict(name);
        }
    }

    /**
     * Enlève un joueur de la salle où il se trouve (lors de /hub, quit…), met à jour la salle,
     * reset son scoreboard, nettoie la laine « Sélectionner équipe »,
     * et supprime la salle si elle devient vide.
     */
    public static void leaveWaitingRoom(Player p) {
        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        Iterator<WaitingRoom> it = rooms.values().iterator();

        while (it.hasNext()) {
            WaitingRoom wr = it.next();
            if (wr.removePlayer(p)) {
                // 1) Maj du scoreboard de la salle
                wr.updateScoreboard();

                // 2) Reset du scoreboard du joueur
                if (mgr != null) p.setScoreboard(mgr.getNewScoreboard());

                // 3) Nettoie la laine "Sélectionner équipe" qui pouvait rester
                cleanupTeamSelector(p);

                // 4) Si salle vide → suppression totale
                if (wr.getParticipants().isEmpty()) {
                    it.remove();
                    String name = wr.getSpawn().getWorld().getName();
                    unloadAndDeleteStrict(name);
                }
                break;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Création / clonage de monde d’attente
    // ------------------------------------------------------------------------

    private static WaitingRoom createRoom() {
        FileConfiguration cfg = Blitz2v2.getInstance().getCfg();
        String template = cfg.getString("waiting.world", "world_waiting");

        int id = NEXT_ID.getAndIncrement();
        String name = template + "_" + id;

        Path src = Bukkit.getWorldContainer().toPath().resolve(template);
        Path dst = Bukkit.getWorldContainer().toPath().resolve(name);

        // En cas de reliquat (crash), tente de purger avant de copier
        deleteFolderWithRetries(dst, 2);

        copyFolder(src, dst);

        // Purger uid.dat et session.lock pour éviter duplicate-world
        try {
            Files.deleteIfExists(dst.resolve("uid.dat"));
            Files.deleteIfExists(dst.resolve("session.lock"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Charge le monde d’attente
        World w = Bukkit.createWorld(new WorldCreator(name));
        if (w == null) {
            Bukkit.getLogger().severe("[Blitz2v2] Impossible de charger le monde '" + name + "' !");
            return null;
        }

        Location spawn = w.getSpawnLocation();
        WaitingRoom wr = new WaitingRoom(id, spawn);
        rooms.put(id, wr);
        return wr;
    }

    // ------------------------------------------------------------------------
    // Helpers fichiers / mondes (versions robustes)
    // ------------------------------------------------------------------------

    private static void copyFolder(Path src, Path dst) {
        try {
            if (!Files.exists(src)) {
                Bukkit.getLogger().severe("[Blitz2v2] Monde template introuvable: " + src);
                return;
            }
            if (Files.exists(dst)) deleteFolderWithRetries(dst, 2);
            Files.walk(src).forEach(p -> {
                try {
                    Path rel = src.relativize(p);
                    Path tgt = dst.resolve(rel);
                    if (Files.isDirectory(p)) {
                        Files.createDirectories(tgt);
                    } else {
                        Files.copy(p, tgt, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Décharge le monde (si chargé), évacue joueurs/entités, puis supprime le dossier avec retries. */
    private static void unloadAndDeleteStrict(String name) {
        World w = Bukkit.getWorld(name);
        if (w != null) {
            // TP éventuels joueurs vers le lobby
            Location lobby = Blitz2v2.getInstance().getLobbySpawn();
            for (Player pl : new ArrayList<>(w.getPlayers())) {
                if (pl.isOnline()) {
                    pl.closeInventory();
                    if (lobby != null) pl.teleport(lobby);
                }
            }

            // Nettoyage entités & chunks
            try { w.setAutoSave(false); } catch (Throwable ignored) {}
            w.getLoadedChunks(); // force l’init si besoin
            w.getLoadedChunks(); // (appel idempotent)

            Arrays.stream(w.getLoadedChunks()).forEach(c -> {
                for (Entity e : c.getEntities()) if (!(e instanceof Player)) e.remove();
                try { w.unloadChunk(c.getX(), c.getZ()); } catch (Throwable ignored) {}
            });

            // Essaye d’unload
            boolean ok = Bukkit.unloadWorld(w, false);
            if (!ok) {
                // second essai 2 ticks plus tard
                Bukkit.getScheduler().runTaskLater(Blitz2v2.getInstance(), () -> {
                    World w2 = Bukkit.getWorld(name);
                    if (w2 != null) Bukkit.unloadWorld(w2, false);
                    deleteFolderWithRetries(Bukkit.getWorldContainer().toPath().resolve(name), 3);
                }, 2L);
                return;
            }
        }
        deleteFolderWithRetries(Bukkit.getWorldContainer().toPath().resolve(name), 3);
    }

    private static void deleteFolderWithRetries(Path dir, int retries) {
        for (int i = 0; i < Math.max(1, retries); i++) {
            deleteFolder(dir);
            if (!Files.exists(dir)) return;
            try { Thread.sleep(50L); } catch (InterruptedException ignored) {}
        }
        deleteFolder(dir);
    }

    private static void deleteFolder(Path dir) {
        try {
            if (!Files.exists(dir)) return;
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Nettoie la laine "Sélectionner équipe" si le joueur l’a encore (cas /hub, /spawn, etc.)
    private static void cleanupTeamSelector(Player p) {
        Arrays.stream(p.getInventory().getContents())
                .filter(it -> it != null
                        && it.getType() == org.bukkit.Material.WOOL
                        && it.hasItemMeta()
                        && "§eSélectionner équipe".equals(it.getItemMeta().getDisplayName()))
                .forEach(it -> p.getInventory().remove(it));
        p.updateInventory();
    }
}
