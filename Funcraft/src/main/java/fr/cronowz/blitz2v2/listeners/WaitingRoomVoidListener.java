// src/main/java/fr/cronowz/blitz2v2/listeners/WaitingRoomVoidListener.java
package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.PartyManager;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class WaitingRoomVoidListener implements Listener {

    private final int    thresholdY;
    private final String waitingBase;

    public WaitingRoomVoidListener() {
        this.thresholdY  = Blitz2v2.getInstance().getCfg().getInt("waiting.void-y", 0);           // configurable (par défaut 0)
        this.waitingBase = Blitz2v2.getInstance().getCfg().getString("waiting.world","world_waiting").toLowerCase();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;

        Player p = e.getPlayer();
        WaitingRoom wr = PartyManager.getParty(p);
        if (wr == null) return; // pas en salle d'attente

        World w = p.getWorld();
        String n = w.getName().toLowerCase();
        // sécurités : seulement dans un monde "world_waiting" ou "world_waiting_*"
        if (!(n.equals(waitingBase) || n.startsWith(waitingBase + "_"))) return;

        // On déclenche quand on tombe sous le seuil (mouvement descendant)
        if (e.getTo().getY() <= thresholdY && e.getFrom().getY() > e.getTo().getY()) {
            Location spawn = wr.getSpawn();
            if (spawn != null && spawn.getWorld() != null) {
                p.setFallDistance(0f);
                p.teleport(spawn);
            }
        }
    }
}
