// src/main/java/fr/cronowz/blitz2v2/manager/GameManager.java
package fr.cronowz.blitz2v2.manager;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.game.GameSession;
import fr.cronowz.blitz2v2.game.KitService;
import fr.cronowz.blitz2v2.game.KitService.TeamColor;
import fr.cronowz.blitz2v2.listeners.JoinListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;

public class GameManager {

    private static Location RED_SPAWN;
    private static Location BLUE_SPAWN;

    /** Sessions actives, key = waiting-room ID (ou -1 pour solo) */
    private final Map<Integer, GameSession> sessions = new HashMap<>();

    /** Mondes de game actifs (ex: world_game_1, world_game_2…) */
    private final Set<String> activeWorlds =
            Collections.synchronizedSet(new HashSet<>());

    /** Démarre une partie pour la waiting-room wr (ou solo si wr==null). */
    public void startGame(WaitingRoom wr, List<Player> participants) {
        FileConfiguration cfg = Blitz2v2.getInstance().getCfg();
        String template   = cfg.getString("game.world", "world_game");
        String backup     = cfg.getString("game.backup-folder", "world_game_backup");
        int    sessionId  = (wr != null ? wr.getId() : -1);
        String worldName  = sessionId >= 0 ? template + "_" + sessionId : template + "_solo";

        // Teams pour le tab
        Scoreboard mainSb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team red = mainSb.getTeam("RED");
        if (red == null) {
            red = mainSb.registerNewTeam("RED");
            red.setPrefix("§c★ ");
            red.setAllowFriendlyFire(false);
        }
        Team blue = mainSb.getTeam("BLUE");
        if (blue == null) {
            blue = mainSb.registerNewTeam("BLUE");
            blue.setPrefix("§9★ ");
            blue.setAllowFriendlyFire(false);
        }

        // Unload si déjà présent
        unloadIfLoaded(worldName);

        // Clone du backup
        try {
            cloneFromBackup(backup, worldName);
        } catch (IOException ex) {
            Blitz2v2.getInstance().getLogger().severe(
                    "[Blitz2v2] Échec du clonage de '" + backup + "' vers '" + worldName + "': " + ex.getMessage());
            return;
        }

        // Charge le monde
        World gameWorld = Bukkit.createWorld(new WorldCreator(worldName));
        if (gameWorld == null) {
            Blitz2v2.getInstance().getLogger()
                    .severe("[Blitz2v2] Impossible de charger le monde '" + worldName + "' !");
            return;
        }
        // Enregistre ce monde comme “actif”
        activeWorlds.add(worldName);

        // Spawns d’équipes
        RED_SPAWN  = parseLoc(cfg.getString("spawns.red"),  gameWorld);
        BLUE_SPAWN = parseLoc(cfg.getString("spawns.blue"), gameWorld);
        if (RED_SPAWN == null || BLUE_SPAWN == null) {
            RED_SPAWN = BLUE_SPAWN = gameWorld.getSpawnLocation();
        }

        // Répartit les RANDOM restants
        if (wr != null) wr.assignRandomTeams();

        // TP + kit + effets + teams
        ScoreboardManager sbMgr = Bukkit.getScoreboardManager();
        KitService kit = Blitz2v2.getInstance().getKitService();
        for (Player p : participants) {
            Location target = gameWorld.getSpawnLocation();
            if (wr != null) {
                if (wr.getRedTeam().contains(p)) target = RED_SPAWN;
                else if (wr.getBlueTeam().contains(p)) target = BLUE_SPAWN;
            }

            if (sbMgr != null) p.setScoreboard(sbMgr.getNewScoreboard());
            p.teleport(target);
            p.setBedSpawnLocation(target, true);
            p.setGameMode(GameMode.SURVIVAL);
            p.setInvulnerable(false);

            TeamColor color = (wr != null && wr.getRedTeam().contains(p))
                    ? TeamColor.RED : TeamColor.BLUE;
            kit.giveKit(p, color);

            p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION,        3*20, 255, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,   3*20,   1, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 3*20,   1, true));

            if (wr != null) {
                if (wr.getRedTeam().contains(p)) red.addEntry(p.getName());
                else if (wr.getBlueTeam().contains(p)) blue.addEntry(p.getName());
            }
        }

        // Démarre la session
        GameSession session = new GameSession(wr, participants, gameWorld);
        sessions.put(sessionId, session);
        session.start();
    }

    /**
     * Termine une partie : annule la session, TP lobby, supprime la salle d’attente,
     * puis unload+delete du monde de jeu (strict).
     */
    public void endGame(WaitingRoom wr, List<Player> winners) {
        int sessionId = (wr != null ? wr.getId() : -1);
        GameSession session = sessions.remove(sessionId);
        if (session == null) return;

        session.stopImmediately();

        List<Player> all    = (wr != null) ? new ArrayList<>(wr.getParticipants())
                : new ArrayList<>(winners);
        List<Player> losers = new ArrayList<>(all);
        losers.removeAll(winners);

        winners.forEach(p -> { if (p.isOnline()) p.setInvulnerable(true); });
        losers.forEach(p  -> { if (p.isOnline()) p.setGameMode(GameMode.SPECTATOR); });

        new BukkitRunnable() {
            @Override public void run() {
                // Enlève les entrées tab
                Scoreboard mainSb = Bukkit.getScoreboardManager().getMainScoreboard();
                Team red = mainSb.getTeam("RED");
                Team blue = mainSb.getTeam("BLUE");
                for (Player p : all) {
                    if (red  != null) red.removeEntry(p.getName());
                    if (blue != null) blue.removeEntry(p.getName());
                }

                // Retour lobby + reset
                ScoreboardManager sbMgr = Bukkit.getScoreboardManager();
                Location lobby = getLobbySpawn();
                for (Player p : all) {
                    if (!p.isOnline()) continue;
                    p.teleport(lobby);
                    p.getInventory().clear();
                    p.getInventory().setArmorContents(new ItemStack[4]);

                    ItemStack compass = new ItemStack(Material.COMPASS);
                    ItemMeta cm = compass.getItemMeta();
                    cm.setDisplayName("§eMenu des parties");
                    compass.setItemMeta(cm);
                    p.getInventory().setItem(0, compass);

                    JoinListener.showLobbyBoard(p);
                    p.setGameMode(GameMode.SURVIVAL);
                    p.setInvulnerable(false);
                }

                // *** NOUVEAU : supprime la salle d’attente (liste & monde waiting) ***
                if (wr != null) {
                    WaitingRoomManager.removeRoom(wr.getId());
                }

                // Puis nettoie le monde de jeu
                FileConfiguration cfg = Blitz2v2.getInstance().getCfg();
                String template = cfg.getString("game.world","world_game");
                String wName = sessionId >= 0 ? template + "_" + sessionId : template + "_solo";

                // On sort ce monde de la liste active, puis on le détruit
                activeWorlds.remove(wName);

                Bukkit.getScheduler().runTaskLater(Blitz2v2.getInstance(),
                        () -> unloadAndDeleteStrict(wName), 1L);
            }
        }.runTaskLater(Blitz2v2.getInstance(), 6*20L);
    }

    /** À appeler dès qu’un joueur quitte la partie (/hub, quit, kick…) */
    public void handlePlayerExit(Player p) {
        Scoreboard blank = getBlankScoreboard();
        if (blank != null) p.setScoreboard(blank);

        GameSession session = getSessionForPlayer(p);
        if (session == null) return;
        WaitingRoom wr = session.getWaitingRoom();

        session.removePlayer(p);
        wr.removePlayer(p);

        boolean redEmpty  = wr.getRedTeam().stream().noneMatch(Player::isOnline);
        boolean blueEmpty = wr.getBlueTeam().stream().noneMatch(Player::isOnline);
        if (redEmpty && !blueEmpty) {
            session.forceEnd(TeamColor.BLUE);
        } else if (blueEmpty && !redEmpty) {
            session.forceEnd(TeamColor.RED);
        }
    }

    public GameSession getSessionForPlayer(Player p) {
        for (GameSession sess : sessions.values()) {
            if (sess.getPlayers().contains(p)) return sess;
        }
        return null;
    }

    public Scoreboard getBlankScoreboard() {
        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        return (mgr != null) ? mgr.getNewScoreboard() : null;
    }

    /** Vrai si le monde passé est un monde de game géré par ce plugin. */
    public boolean isGameWorld(World w) {
        return w != null && activeWorlds.contains(w.getName());
    }

    /** Forcer l’unload + delete d’un monde (utilisé par WaitingRoomManager & purge). */
    public void forceUnloadAndDeleteWorld(String name) {
        if (name == null || name.isEmpty()) return;
        activeWorlds.remove(name);
        unloadAndDeleteStrict(name);
    }

    // ─── UTILITAIRES ────────────────────────────────────────────────────────────

    private void unloadIfLoaded(String name) {
        World w = Bukkit.getWorld(name);
        if (w != null) Bukkit.unloadWorld(w, false);
    }

    private void unloadAndDelete(String name) {
        unloadIfLoaded(name);
        deleteFolder(Bukkit.getWorldContainer().toPath().resolve(name));
    }

    /** Version robuste : purge entités/chunks, unload, retry, delete avec retries. */
    private void unloadAndDeleteStrict(String name) {
        World w = Bukkit.getWorld(name);
        if (w != null) {
            Location lobby = getLobbySpawn();
            for (Player p : new ArrayList<>(w.getPlayers())) {
                if (p.isOnline()) {
                    p.closeInventory();
                    p.teleport(lobby);
                }
            }
            try { w.setAutoSave(false); } catch (Throwable ignored) {}

            for (Chunk c : w.getLoadedChunks()) {
                for (Entity e : c.getEntities()) if (!(e instanceof Player)) e.remove();
                try { w.unloadChunk(c.getX(), c.getZ()); } catch (Throwable ignored) {}
            }

            boolean ok = Bukkit.unloadWorld(w, false);
            if (!ok) {
                Bukkit.getScheduler().runTaskLater(Blitz2v2.getInstance(), () -> {
                    World w2 = Bukkit.getWorld(name);
                    if (w2 != null) {
                        for (Chunk c : w2.getLoadedChunks()) {
                            for (Entity e : c.getEntities()) if (!(e instanceof Player)) e.remove();
                            try { w2.unloadChunk(c.getX(), c.getZ()); } catch (Throwable ignored) {}
                        }
                        Bukkit.unloadWorld(w2, false);
                    }
                    deleteFolderWithRetries(Bukkit.getWorldContainer().toPath().resolve(name), 3);
                }, 2L);
                return;
            }
        }
        deleteFolderWithRetries(Bukkit.getWorldContainer().toPath().resolve(name), 3);
    }

    private void cloneFromBackup(String srcName, String dstName) throws IOException {
        Path src = Bukkit.getWorldContainer().toPath().resolve(srcName);
        Path dst = Bukkit.getWorldContainer().toPath().resolve(dstName);
        if (Files.exists(dst)) deleteFolder(dst);
        Files.walk(src).forEach(path -> {
            try {
                Path rel = src.relativize(path);
                Path tgt = dst.resolve(rel);
                if (Files.isDirectory(path)) Files.createDirectories(tgt);
                else Files.copy(path, tgt, StandardCopyOption.COPY_ATTRIBUTES);
            } catch (IOException e) { throw new UncheckedIOException(e); }
        });
        Files.deleteIfExists(dst.resolve("uid.dat"));
        Files.deleteIfExists(dst.resolve("session.lock"));
    }

    private static void deleteFolder(Path dir) {
        try {
            if (!Files.exists(dir)) return;
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> p.toFile().delete());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void deleteFolderWithRetries(Path dir, int retries) {
        for (int i = 0; i < Math.max(1, retries); i++) {
            deleteFolder(dir);
            if (!Files.exists(dir)) return;
            try { Thread.sleep(50L); } catch (InterruptedException ignored) {}
        }
        deleteFolder(dir);
    }

    private Location getLobbySpawn() {
        FileConfiguration cfg = Blitz2v2.getInstance().getCfg();
        World w = Bukkit.getWorld(cfg.getString("lobby.world","world_lobby"));
        double x = cfg.getDouble("lobby.x",0);
        double y = cfg.getDouble("lobby.y",64);
        double z = cfg.getDouble("lobby.z",0);
        return new Location(w,x,y,z);
    }

    private Location parseLoc(String raw, World w) {
        if (raw == null || w == null) return w.getSpawnLocation();
        try {
            String[] s = raw.replace(" ","").split(",");
            double x = Double.parseDouble(s[0]),
                    y = Double.parseDouble(s[1]),
                    z = Double.parseDouble(s[2]);
            float yaw   = s.length>3 ? Float.parseFloat(s[3]) : 0f;
            float pitch = s.length>4 ? Float.parseFloat(s[4]) : 0f;
            return new Location(w,x,y,z,yaw,pitch);
        } catch (Exception ex) { return w.getSpawnLocation(); }
    }

    public static Location getRedSpawn()  { return RED_SPAWN; }
    public static Location getBlueSpawn() { return BLUE_SPAWN; }
}
