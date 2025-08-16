// src/main/java/fr/cronowz/blitz2v2/manager/WaitingRoom.java
package fr.cronowz.blitz2v2.manager;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.listeners.TeamSelectListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WaitingRoom {

    public enum Team { RED, BLUE, RANDOM }

    private final int id;
    private final Location spawn;
    private final List<Player> participants = new ArrayList<>();
    private final List<Player> redTeam    = new ArrayList<>();
    private final List<Player> blueTeam   = new ArrayList<>();
    private boolean started = false;
    private BukkitTask countdownTask;
    private int countdown;

    public WaitingRoom(int id, Location spawn) {
        this.id = id;
        this.spawn = spawn;
    }

    public int getId() { return id; }
    public Location getSpawn() { return spawn; }

    /** Snapshots pour éviter toute modification externe. */
    public List<Player> getParticipants() { return new ArrayList<>(participants); }
    public List<Player> getRedTeam()      { return new ArrayList<>(redTeam); }
    public List<Player> getBlueTeam()     { return new ArrayList<>(blueTeam); }

    public boolean isFull() {
        return participants.size() >=
                Blitz2v2.getInstance().getCfg().getInt("countdown.max-players", 4);
    }
    public boolean isStarted() { return started; }

    /** Retire un joueur de la salle ET de son équipe. */
    public synchronized boolean removePlayer(Player p) {
        boolean removed = participants.remove(p);
        redTeam.remove(p);
        blueTeam.remove(p);
        return removed;
    }

    /** Ajoute un joueur en file d'attente. */
    public synchronized boolean addPlayer(Player p) {
        if (started || participants.contains(p) || isFull()) return false;

        participants.add(p);
        p.teleport(spawn);

        // Nettoie & donne le sélecteur d’équipe
        p.getInventory().clear();
        TeamSelectListener.giveSelector(p);

        updateScoreboard();

        int min = Blitz2v2.getInstance().getCfg().getInt("countdown.min-players", 3);
        if (participants.size() >= min) {
            countdown = Blitz2v2.getInstance().getCfg()
                    .getInt(participants.size() == 4
                            ? "countdown.time-4-players"
                            : "countdown.time-3-players");
            startCountdown();
        }
        return true;
    }

    /** Assigne un joueur à une équipe. */
    public synchronized void assignTeam(Player p, Team team) {
        redTeam.remove(p);
        blueTeam.remove(p);
        if (team == Team.RED && redTeam.size() < 2) {
            redTeam.add(p);
        } else if (team == Team.BLUE && blueTeam.size() < 2) {
            blueTeam.add(p);
        }
        // RANDOM => assigné plus tard par assignRandomTeams()
    }

    /** Répartit les joueurs “random” restants dans les deux équipes. */
    public synchronized void assignRandomTeams() {
        List<Player> random = new ArrayList<>();
        for (Player p : participants) {
            if (!redTeam.contains(p) && !blueTeam.contains(p)) {
                random.add(p);
            }
        }
        Collections.shuffle(random);
        for (Player p : random) {
            if (redTeam.size() < 2)       redTeam.add(p);
            else if (blueTeam.size() < 2) blueTeam.add(p);
        }
    }

    // ───────────────────────────── COMPTE À REBOURS + POPUP/SONS ─────────────────────────────
    private synchronized void startCountdown() {
        if (countdownTask != null) return;
        countdownTask = new BukkitRunnable() {
            @Override public void run() {
                int min = Blitz2v2.getInstance().getCfg().getInt("countdown.min-players", 3);

                // plus assez de joueurs -> on stoppe proprement
                if (participants.size() < min) {
                    cancelCountdown();
                    updateScoreboard();
                    return;
                }

                // Affichage/son pendant le décompte (5..1)
                if (countdown <= 5 && countdown > 0) {
                    broadcastCountdown(countdown);
                }

                // Fin du décompte -> GO! + lancement au tick suivant
                if (countdown-- <= 0) {
                    cancelCountdown();
                    started = true;

                    broadcastGo(); // titre + son

                    // Lancer au prochain tick (évite soucis d’ordonnancement)
                    List<Player> toStart = new ArrayList<>(participants);
                    Bukkit.getScheduler().runTask(Blitz2v2.getInstance(), () ->
                            Blitz2v2.getInstance()
                                    .getGameManager()
                                    .startGame(WaitingRoom.this, toStart)
                    );
                    return;
                }

                // MAJ scoreboard (affiche le temps restant)
                updateScoreboard();
            }
        }.runTaskTimer(Blitz2v2.getInstance(), 0L, 20L);
    }

    private synchronized void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    /** Popup + petit ding sur les 5 dernières secondes. */
    private void broadcastCountdown(int secondsLeft) {
        for (Player p : new ArrayList<>(participants)) {
            if (!p.isOnline()) continue;
            p.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + secondsLeft,
                    ChatColor.YELLOW + "Début dans " + secondsLeft + "s");
            float pitch = (secondsLeft == 1 ? 1.6f : 1.0f);
            play(p,
                    new String[]{"BLOCK_NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING"},
                    1.0f, pitch
            );
        }
    }

    /** “GO!” + son de level-up. */
    private void broadcastGo() {
        for (Player p : new ArrayList<>(participants)) {
            if (!p.isOnline()) continue;
            p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "GO!",
                    ChatColor.GRAY + "Bonne chance ✦");
            play(p, new String[]{"ENTITY_PLAYER_LEVELUP", "LEVEL_UP"}, 1.0f, 1.0f);
        }
    }

    /**
     * Joue le premier son existant parmi la liste de candidats
     * (compat multi-versions des constantes Sound).
     */
    private void play(Player p, String[] candidates, float volume, float pitch) {
        try {
            for (String name : candidates) {
                try {
                    Sound s = Sound.valueOf(name);
                    p.playSound(p.getLocation(), s, volume, pitch);
                    return;
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (Throwable ignored) { }
    }
    // ────────────────────────────────────────────────────────────────────────────

    /** Met à jour le scoreboard de chaque joueur (snapshot pour éviter CME). */
    public void updateScoreboard() {
        FileConfiguration cfg = Blitz2v2.getInstance().getCfg();
        int minPlayers = cfg.getInt("countdown.min-players", 3);
        int maxPlayers = cfg.getInt("countdown.max-players", 4);

        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;

        List<Player> snapshot = new ArrayList<>(participants);
        for (Player p : snapshot) {
            if (!p.isOnline()) continue;

            Scoreboard board = mgr.getNewScoreboard();
            Objective obj = board.registerNewObjective("waiting" + id, "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Plexy.net");

            obj.getScore(" ").setScore(4);
            obj.getScore(ChatColor.WHITE + "Jeu : " + ChatColor.YELLOW + "Blitz").setScore(3);
            obj.getScore(
                    ChatColor.WHITE + "Joueurs : " +
                            ChatColor.GREEN + participants.size() +
                            ChatColor.GRAY + "/" + maxPlayers
            ).setScore(2);

            if (participants.size() < minPlayers) {
                obj.getScore(ChatColor.RED + "Pas assez de joueurs").setScore(1);
            } else {
                obj.getScore(
                        ChatColor.WHITE + "Début dans " +
                                ChatColor.YELLOW + countdown + "s"
                ).setScore(1);
            }
            p.setScoreboard(board);
        }
    }
}
