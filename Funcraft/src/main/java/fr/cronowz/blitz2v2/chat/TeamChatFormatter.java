// src/main/java/fr/cronowz/blitz2v2/chat/TeamChatFormatter.java
package fr.cronowz.blitz2v2.chat;

import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Team;

public final class TeamChatFormatter {
    private TeamChatFormatter() {}

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
