// src/main/java/fr/cronowz/blitz2v2/chat/TeamChatFormatter.java
package fr.cronowz.blitz2v2.chat;

import fr.cronowz.blitz2v2.PartyManager;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public final class TeamChatFormatter {
    private TeamChatFormatter() {}

    /**
     * Couleur d'un joueur selon son équipe (rouge/bleu) ; gris sinon.
     */
    public static ChatColor teamColor(Player p) {
        if (p == null) return ChatColor.GRAY;
        WaitingRoom wr = PartyManager.getParty(p);
        if (wr != null) {
            if (wr.getRedTeam().contains(p))  return ChatColor.RED;
            if (wr.getBlueTeam().contains(p)) return ChatColor.BLUE;
        }
        return ChatColor.GRAY;
    }

    /**
     * Retourne le pseudo du joueur coloré selon son équipe.
     */
    public static String coloredName(Player p) {
        if (p == null) return ChatColor.GRAY + "un joueur";
        return teamColor(p) + p.getName();
    }

    /**
     * Détermine la couleur d'une équipe Bukkit.
     * Conserve l'ancien comportement pour compatibilité éventuelle.
     */
    public static ChatColor teamColor(Team t) {
        if (t == null) return ChatColor.GRAY;

        // Essayer d'extraire la couleur depuis le prefix de l'équipe (TAB met souvent la couleur dedans)
        String prefix = "";
        try { prefix = t.getPrefix(); } catch (Throwable ignored) {}
        if (prefix != null && !prefix.isEmpty()) {
            String last = ChatColor.getLastColors(prefix);
            if (last != null && !last.isEmpty()) {
                char code = last.charAt(last.length() - 1);
                ChatColor c = ChatColor.getByChar(code);
                if (c != null) return c;
            }
        }

        // Fallback avec le nom de l'équipe
        String n = t.getName().toLowerCase();
        if (n.contains("red") || n.contains("rouge")) return ChatColor.RED;
        if (n.contains("blue") || n.contains("bleu")) return ChatColor.BLUE;

        return ChatColor.GRAY;
    }
}
