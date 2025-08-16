// src/main/java/fr/cronowz/blitz2v2/listeners/JoinQuitSuppressListener.java
package fr.cronowz.blitz2v2.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitSuppressListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Annule le message de join
        e.setJoinMessage(null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // Annule le message de quit
        e.setQuitMessage(null);
    }
}
