package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.PartyManager;
import fr.cronowz.blitz2v2.manager.GameManager;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameLeaveListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        handleLeave(e.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        handleLeave(e.getPlayer());
    }

    private void handleLeave(Player p) {
        // Si on n'est pas en cours de partie, on ignore
        String gameWorld = Blitz2v2.getInstance().getGameWorldName();
        if (p.getWorld() == null
         || !p.getWorld().getName().equalsIgnoreCase(gameWorld)) {
            return;
        }

        WaitingRoom wr = PartyManager.getParty(p);
        if (wr == null) return;

        // 1) On retire le joueur de la WaitingRoom
        wr.removePlayer(p);

        // 2) On vérifie si une équipe est complètement vide
        boolean redEmpty  = wr.getRedTeam().stream().noneMatch(Player::isOnline);
        boolean blueEmpty = wr.getBlueTeam().stream().noneMatch(Player::isOnline);

        // 3) Si rouge vide → bleu gagne ; si bleu vide → rouge gagne
        if (redEmpty && !blueEmpty) {
            Blitz2v2.getInstance().getGameManager().endGame(wr, wr.getBlueTeam());
        } else if (blueEmpty && !redEmpty) {
            Blitz2v2.getInstance().getGameManager().endGame(wr, wr.getRedTeam());
        }
    }
}
