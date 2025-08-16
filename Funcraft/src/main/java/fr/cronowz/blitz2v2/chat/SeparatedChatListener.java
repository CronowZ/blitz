// src/main/java/fr/cronowz/blitz2v2/chat/SeparatedChatListener.java
package fr.cronowz.blitz2v2.chat;

import fr.cronowz.blitz2v2.Blitz2v2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

public class SeparatedChatListener implements Listener {
    private final Blitz2v2 plugin;

    public SeparatedChatListener(Blitz2v2 plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String raw = e.getMessage();

        String world = p.getWorld().getName();
        boolean isLobby   = world.toLowerCase().contains("lobby");
        boolean isWaiting = world.toLowerCase().contains("waiting");

        String base = plugin.getGameWorldName();
        String wl   = world.toLowerCase();
        String bl   = base.toLowerCase();
        boolean isGame = wl.equals(bl) || wl.startsWith(bl + "_") || wl.startsWith(bl + "-");

        // Laisser le chat vanilla ailleurs
        if (!(isLobby || isWaiting || isGame)) return;

        e.setCancelled(true);

        if (isLobby) {
            broadcastToWorld(p, ChatColor.GOLD + "[Lobby] "
                    + ChatColor.YELLOW + p.getName()
                    + ChatColor.GRAY + " : " + ChatColor.WHITE + raw);
            return;
        }
        if (isWaiting) {
            broadcastToWorld(p, ChatColor.AQUA + "[Attente] "
                    + ChatColor.YELLOW + p.getName()
                    + ChatColor.GRAY + " : " + ChatColor.WHITE + raw);
            return;
        }

        // === En jeu ===
        boolean global = false;
        String msg = raw;
        if (raw.startsWith("@")) {
            global = true;
            msg = raw.substring(1).trim();
        }

        Team team = findTeam(p);
        ChatColor tc = TeamChatFormatter.teamColor(team);
        String tag  = global ? ChatColor.DARK_AQUA + "[Global]" : tc + "[Équipe]";
        String name = tc + p.getName();

        if (global) {
            broadcastToGameWorld(p, base, ChatColor.GRAY + tag + " " + name + ChatColor.GRAY + " : " + ChatColor.WHITE + msg);
        } else {
            broadcastToTeam(p, team, ChatColor.GRAY + tag + " " + name + ChatColor.GRAY + " : " + ChatColor.WHITE + msg);
        }
    }

    private void broadcastToWorld(Player sender, String line) {
        for (Player r : sender.getWorld().getPlayers()) {
            r.sendMessage(line);
        }
    }

    private void broadcastToGameWorld(Player sender, String base, String line) {
        String wl = sender.getWorld().getName().toLowerCase();
        String bl = base.toLowerCase();
        if (!(wl.equals(bl) || wl.startsWith(bl + "_") || wl.startsWith(bl + "-"))) return;

        for (Player r : sender.getWorld().getPlayers()) {
            r.sendMessage(line);
        }
    }

    private void broadcastToTeam(Player sender, Team team, String line) {
        if (team == null) {
            sender.sendMessage(ChatColor.RED + "Vous n'êtes dans aucune équipe : message non envoyé. Utilisez @ pour parler en global.");
            return;
        }
        List<Player> recipients = sameTeamPlayers(sender.getWorld().getPlayers(), team);
        for (Player r : recipients) {
            r.sendMessage(line);
        }
    }

    private List<Player> sameTeamPlayers(List<Player> inWorld, Team team) {
        List<Player> out = new ArrayList<Player>();
        for (Player p : inWorld) {
            if (isInTeam(p, team)) out.add(p);
        }
        return out;
    }

    private boolean isInTeam(Player p, Team team) {
        if (p == null || team == null) return false;
        try {
            // 1.9 API: Team#hasPlayer(OfflinePlayer)
            return team.hasPlayer(p);
        } catch (Throwable t) {
            try {
                OfflinePlayer off = Bukkit.getOfflinePlayer(p.getUniqueId());
                return team.hasPlayer(off);
            } catch (Throwable ignored) {
                Team t2 = findTeam(p);
                return t2 != null && t2.getName().equals(team.getName());
            }
        }
    }

    private Team findTeam(Player p) {
        try {
            Scoreboard sb = p.getScoreboard();
            if (sb == null) sb = Bukkit.getScoreboardManager().getMainScoreboard();
            if (sb != null) {
                for (Team t : sb.getTeams()) {
                    try {
                        if (t.hasPlayer(p)) return t;
                    } catch (Throwable ignored) {
                        try {
                            OfflinePlayer off = Bukkit.getOfflinePlayer(p.getUniqueId());
                            if (t.hasPlayer(off)) return t;
                        } catch (Throwable ignored2) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
