// src/main/java/fr/cronowz/blitz2v2/PartyManager.java
package fr.cronowz.blitz2v2;

import fr.cronowz.blitz2v2.manager.WaitingRoom;
import fr.cronowz.blitz2v2.manager.WaitingRoomManager;
import org.bukkit.entity.Player;

import java.util.List;

public class PartyManager {

    /**
     * Tente d’auto-joindre le joueur à une waiting-room (ou en crée une nouvelle).
     * @return true si join réussi
     */
    public static boolean joinAny(Player p) {
        return WaitingRoomManager.autoJoin(p);
    }

    /**
     * Force le joueur à quitter *toutes* les waiting-rooms où il se trouve.
     */
    public static void leaveAll(Player p) {
        WaitingRoomManager.leaveWaitingRoom(p);
    }

    /**
     * Récupère la waiting-room où se trouve actuellement le joueur, ou null.
     */
    public static WaitingRoom getParty(Player p) {
        List<WaitingRoom> rooms = WaitingRoomManager.getRooms();
        for (WaitingRoom wr : rooms) {
            if (wr.getParticipants().contains(p)) {
                return wr;
            }
        }
        return null;
    }
}
