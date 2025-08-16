// src/main/java/fr/cronowz/blitz2v2/listeners/CommandRestrictionListener.java
package fr.cronowz.blitz2v2.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandRestrictionListener implements Listener {
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (p.isOp()) return;  // OP peut tout faire

        String msg = e.getMessage().toLowerCase();
        if (msg.startsWith("/hub") || msg.startsWith("/spawn")) {
            return;  // autorisé
        }

        e.setCancelled(true);
        p.sendMessage("§cCommande interdite. Seuls /hub et /spawn sont disponibles.");
    }
}
