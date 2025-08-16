// src/main/java/fr/cronowz/blitz2v2/listeners/PartyListener.java
package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.PartyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PartyListener implements Listener {
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        PartyManager.leaveAll(e.getPlayer());
    }
}
