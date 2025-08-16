// src/main/java/fr/cronowz/blitz2v2/chat/SeparatedChatListener.java
package fr.cronowz.blitz2v2.chat;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.PartyManager;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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

        WaitingRoom wr = PartyManager.getParty(p);
        if (wr == null && !global) {
            p.sendMessage(ChatColor.RED + "Vous n'êtes dans aucune équipe : message non envoyé. Utilisez @ pour parler en global.");
            return;
        }

        ChatColor tc = TeamChatFormatter.teamColor(p);
        String tag  = global ? ChatColor.DARK_AQUA + "[Global]" : tc + "[Équipe]";
        String name = tc + p.getName();

        if (global) {
            broadcastToGameWorld(p, base, ChatColor.GRAY + tag + " " + name + ChatColor.GRAY + " : " + ChatColor.WHITE + msg);
        } else {
            List<Player> recipients = wr.getRedTeam().contains(p) ? wr.getRedTeam() : wr.getBlueTeam();
            for (Player r : new ArrayList<>(recipients)) {
                if (r.getWorld().equals(p.getWorld())) {
                    r.sendMessage(ChatColor.GRAY + tag + " " + name + ChatColor.GRAY + " : " + ChatColor.WHITE + msg);
                }
            }
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
}
