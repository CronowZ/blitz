// src/main/java/fr/cronowz/blitz2v2/listeners/QuickExitListener.java
package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.manager.WaitingRoomManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class QuickExitListener implements Listener {

    @EventHandler
    public void onPreCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage().toLowerCase();
        // si le joueur tape exactement /hub ou /spawn
        if (msg.equals("/hub") || msg.equals("/spawn")) {
            Player p = e.getPlayer();
            // le retire de sa waiting-room s’il en a une
            WaitingRoomManager.leaveWaitingRoom(p);
        }
    }
}
