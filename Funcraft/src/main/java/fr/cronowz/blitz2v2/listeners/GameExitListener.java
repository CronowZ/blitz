// src/main/java/fr/cronowz/blitz2v2/listeners/GameExitListener.java
package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.manager.GameManager;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import fr.cronowz.blitz2v2.game.GameSession;
import fr.cronowz.blitz2v2.game.KitService.TeamColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameExitListener implements Listener {

    private final GameManager gm = Blitz2v2.getInstance().getGameManager();

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        handleExit(e.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        handleExit(e.getPlayer());
    }

    private void handleExit(Player p) {
        // 1) Récupère la session de jeu
        GameSession session = gm.getSessionForPlayer(p);
        if (session == null) return;

        // 2) Remet un scoreboard vierge
        if (gm.getBlankScoreboard() != null) {
            p.setScoreboard(gm.getBlankScoreboard());
        }

        // 3) Retire le joueur **de la session** (pour arrêter le refresh)
        session.removePlayer(p);

        // 4) Retire le joueur **de la waiting-room**
        WaitingRoom wr = session.getWaitingRoom();
        wr.removePlayer(p);

        // 5) Si toute l’équipe rouge est partie => bleu gagne, et vice-versa
        boolean redEmpty  = wr.getRedTeam().stream().noneMatch(Player::isOnline);
        boolean blueEmpty = wr.getBlueTeam().stream().noneMatch(Player::isOnline);

        if (redEmpty && !blueEmpty) {
            session.end(TeamColor.BLUE);
        } else if (blueEmpty && !redEmpty) {
            session.end(TeamColor.RED);
        }
    }
}
