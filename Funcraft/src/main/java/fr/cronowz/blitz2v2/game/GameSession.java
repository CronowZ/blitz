// src/main/java/fr/cronowz/blitz2v2/game/GameSession.java
package fr.cronowz.blitz2v2.game;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.manager.GameManager;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import fr.cronowz.blitz2v2.game.KitService.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

public class GameSession implements Listener {

    private final WaitingRoom wr;
    private final List<Player> players;
    private final World gameWorld;
    private final int targetPoints;

    private EnderCrystal redNexus, blueNexus;
    private final Map<TeamColor, Integer> score = new EnumMap<>(TeamColor.class);
    private long startTime;
    private BukkitTask scoreboardUpdater;
    private boolean finished = false;

    public void removePlayer(Player p) { players.remove(p); }
    public void forceEnd(TeamColor winner) { end(winner); }

    public GameSession(WaitingRoom wr, List<Player> participants, World gameWorld) {
        this.wr = wr;
        this.players = new ArrayList<>(participants);
        this.gameWorld = gameWorld;

        FileConfiguration cfg = Blitz2v2.getInstance().getCfg();
        this.targetPoints = cfg.getInt("game.target-points", 4);

        score.put(TeamColor.RED, 0);
        score.put(TeamColor.BLUE, 0);
    }

    public void start() {
        FileConfiguration cfg = Blitz2v2.getInstance().getCfg();

        redNexus  = spawnCrystal(parseLoc(cfg.getString("nexus.red"),  gameWorld));
        blueNexus = spawnCrystal(parseLoc(cfg.getString("nexus.blue"), gameWorld));

        Bukkit.getPluginManager().registerEvents(this, Blitz2v2.getInstance());
        startTime = System.currentTimeMillis();

        players.stream().filter(Player::isOnline)
                .forEach(p -> p.setScoreboard(buildScoreboardFor(p)));

        scoreboardUpdater = new BukkitRunnable() {
            @Override public void run() {
                if (finished) { cancel(); return; }
                players.stream().filter(Player::isOnline)
                        .forEach(p -> p.setScoreboard(buildScoreboardFor(p)));
            }
        }.runTaskTimer(Blitz2v2.getInstance(), 20, 20);
    }

    @EventHandler
    public void onCrystalDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof EnderCrystal
                && e.getEntity().getWorld().equals(gameWorld)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (finished) return;
        Location to = e.getTo();
        if (to == null || !to.getWorld().equals(gameWorld)) return;

        Player p = e.getPlayer();
        TeamColor color = getTeamColor(p);
        if (color == null) return;

        if (color == TeamColor.BLUE && to.distance(redNexus.getLocation()) < 1.0) {
            onPointScored(TeamColor.BLUE, p);
        } else if (color == TeamColor.RED && to.distance(blueNexus.getLocation()) < 1.0) {
            onPointScored(TeamColor.RED, p);
        }
    }

    public void stopImmediately() {
        if (!finished) {
            finished = true;
            if (scoreboardUpdater != null) scoreboardUpdater.cancel();
            HandlerList.unregisterAll(this);
        }
    }

    private void onPointScored(TeamColor byTeam, Player scorer) {
        int newScore = score.get(byTeam) + 1;
        score.put(byTeam, newScore);

        ChatColor teamColor = (byTeam == TeamColor.RED ? ChatColor.RED : ChatColor.AQUA);
        String coreMsg = teamColor + scorer.getName() + ChatColor.WHITE + " a marqué !";

        // Ally: title + subtitle ; Enemy: subtitle only (smaller)
        players.forEach(p -> {
            if (!p.isOnline()) return;
            TeamColor viewerTeam = getTeamColor(p);
            if (viewerTeam == byTeam) {
                p.sendTitle(ChatColor.YELLOW + "+1 point", coreMsg);
            } else {
                p.sendTitle("", coreMsg);
            }
            p.sendMessage("[Blitz] " + coreMsg);
        });

        Location dest = (byTeam == TeamColor.RED ? GameManager.getRedSpawn() : GameManager.getBlueSpawn());
        scorer.teleport(dest);

        if (newScore >= targetPoints) end(byTeam);
    }

    public void end(TeamColor winner) {
        if (finished) return;
        finished = true;

        if (scoreboardUpdater != null) scoreboardUpdater.cancel();
        HandlerList.unregisterAll(this);

        List<Player> winners = new ArrayList<>();
        for (Player p : new ArrayList<>(players)) {
            boolean isRed = wr.getRedTeam().contains(p);
            if ((winner == TeamColor.RED && isRed) || (winner == TeamColor.BLUE && !isRed)) {
                winners.add(p);
                if (p.isOnline()) p.setInvulnerable(true);
            } else if (p.isOnline()) {
                p.setGameMode(org.bukkit.GameMode.SPECTATOR);
            }
        }

        ChatColor wc = (winner == TeamColor.RED ? ChatColor.RED : ChatColor.AQUA);
        Bukkit.broadcastMessage("[Blitz] L'équipe " + wc + winner.name() + ChatColor.WHITE + " a gagné !");
        players.forEach(p -> { if (p.isOnline()) p.sendTitle(ChatColor.GREEN + "Victoire !", wc + "Équipe " + winner.name()); });

        new BukkitRunnable() {
            @Override public void run() {
                Blitz2v2.getInstance().getGameManager().endGame(wr, winners);
            }
        }.runTaskLater(Blitz2v2.getInstance(), 6 * 20L);
    }

    private EnderCrystal spawnCrystal(Location loc) {
        EnderCrystal c = gameWorld.spawn(loc, EnderCrystal.class);
        c.setSilent(true);
        c.setInvulnerable(true);
        return c;
    }

    private Location parseLoc(String raw, World w) {
        if (raw == null || w == null) return w.getSpawnLocation();
        try {
            String[] s = raw.replace(" ", "").split(",");
            double x = Double.parseDouble(s[0]),
                    y = Double.parseDouble(s[1]),
                    z = Double.parseDouble(s[2]);
            float yaw   = (s.length>3 ? Float.parseFloat(s[3]) : 0f),
                    pitch = (s.length>4 ? Float.parseFloat(s[4]) : 0f);
            return new Location(w, x, y, z, yaw, pitch);
        } catch (Exception ex) { return w.getSpawnLocation(); }
    }

    // ─────────────── Scoreboard avec espacements stables ───────────────

    private Scoreboard buildScoreboardFor(Player viewer) {
        TeamColor myTeam = getTeamColor(viewer);

        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        Scoreboard sb = mgr.getNewScoreboard();
        Objective obj = sb.registerNewObjective("game", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Blitz");

        int line = 15;

        obj.getScore(blank(0)).setScore(line--);

        String teamName = (myTeam == TeamColor.RED ? ChatColor.RED + "Rouge" : ChatColor.AQUA + "Bleu");
        obj.getScore(ChatColor.GRAY + "Tu es dans").setScore(line--);
        obj.getScore(ChatColor.GRAY + "l'équipe " + teamName).setScore(line--);

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        obj.getScore(ChatColor.GRAY + "Chrono " + ChatColor.YELLOW +
                String.format("%02d:%02d", elapsed / 60, elapsed % 60) + "s").setScore(line--);

        obj.getScore(blank(1)).setScore(line--);

        obj.getScore(ChatColor.RED  + "Rouge" + ChatColor.GRAY + ": " + ChatColor.WHITE + score.get(TeamColor.RED)  + " points").setScore(line--);
        obj.getScore(ChatColor.AQUA + "Bleu"  + ChatColor.GRAY + ": " + ChatColor.WHITE + score.get(TeamColor.BLUE) + " points").setScore(line--);
        obj.getScore(ChatColor.GRAY + "Objectif: " + ChatColor.WHITE + targetPoints + " points").setScore(line--);

        obj.getScore(blank(2)).setScore(line--);
        obj.getScore(blank(3)).setScore(line--);

        obj.getScore(ChatColor.DARK_GRAY + "Alliances entre").setScore(line--);
        obj.getScore(ChatColor.DARK_GRAY + "équipes interdites").setScore(line--);

        obj.getScore(blank(4)).setScore(line);

        return sb;
    }

    private String blank(int idx) {
        switch (idx) {
            case 0: return ChatColor.RESET.toString();
            case 1: return ChatColor.BLACK.toString();
            case 2: return ChatColor.DARK_BLUE.toString();
            case 3: return ChatColor.DARK_GREEN.toString();
            default: return ChatColor.DARK_RED.toString();
        }
    }

    public TeamColor getTeamColor(Player p) {
        if (wr == null) return null;
        if (wr.getRedTeam().contains(p))  return TeamColor.RED;
        if (wr.getBlueTeam().contains(p)) return TeamColor.BLUE;
        return null;
    }

    public WaitingRoom getWaitingRoom() { return wr; }
    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
}
