// src/main/java/fr/cronowz/blitz2v2/listeners/WaitingRoomListener.java
package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.manager.WaitingRoomManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class WaitingRoomListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        leave(e.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        leave(e.getPlayer());
    }

    private void leave(Player p) {
        WaitingRoomManager.leaveWaitingRoom(p);
    }
}
