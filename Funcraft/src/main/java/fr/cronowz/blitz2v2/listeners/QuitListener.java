package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.manager.GameManager;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import fr.cronowz.blitz2v2.manager.WaitingRoomManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class QuitListener implements Listener {

    /** Clique droit sur la porte “EXIT” pour retourner au lobby / waiting-room */
    @EventHandler
    public void onExitClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR
                && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack it = e.getItem();
        if (it == null
                || it.getType() != Material.WOODEN_DOOR
                || !it.hasItemMeta()
                || !"§cEXIT".equals(it.getItemMeta().getDisplayName())) {
            return;
        }
        e.setCancelled(true);
        Player p = e.getPlayer();

        // 1) Quitter proprement la waiting-room
        WaitingRoomManager.leaveWaitingRoom(p);

        // 2) Téléportation au spawn du lobby
        World lobby = Bukkit.getWorld(Blitz2v2.getInstance().getCfg().getString("lobby.world", "world_lobby"));
        Location spawn = new Location(
                lobby,
                Blitz2v2.getInstance().getCfg().getDouble("lobby.x"),
                Blitz2v2.getInstance().getCfg().getDouble("lobby.y"),
                Blitz2v2.getInstance().getCfg().getDouble("lobby.z")
        );
        p.teleport(spawn);

        // 3) Remise en survie / reset scoreboard
        p.setGameMode(GameMode.SURVIVAL);
        if (Bukkit.getScoreboardManager() != null) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        // 4) Confirmation
        p.sendMessage("§aVous êtes retourné·e au lobby !");
    }

    /** Si on quitte/le kick mid-game, on retire du WaitingRoom et on vérifie la victoire */
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        handleQuit(e.getPlayer());
    }
    @EventHandler
    public void onKick(PlayerKickEvent e) {
        handleQuit(e.getPlayer());
    }

    private void handleQuit(Player p) {
        // on récupère la WR (elle existe encore si la partie est ongoing)
        WaitingRoom wr = WaitingRoomManager.getRooms().stream()
                .filter(r -> r.getParticipants().contains(p))
                .findFirst().orElse(null);
        if (wr == null) {
            // pas en waiting-room ni en partie
            return;
        }

        // on retire le joueur de la salle et de son équipe
        wr.removePlayer(p);
        // on reset son scoreboard
        if (Bukkit.getScoreboardManager() != null) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        // S’il s’agissait d’une partie déjà démarrée…
        if (wr.isStarted()) {
            // calcule les restants par équipe
            List<Player> red  = wr.getRedTeam();
            List<Player> blue = wr.getBlueTeam();

            // si l’une des équipes est vide → l’autre gagne
            if (red.isEmpty() && !blue.isEmpty()) {
                endGameAndAnnounce(wr, blue, p);
            } else if (blue.isEmpty() && !red.isEmpty()) {
                endGameAndAnnounce(wr, red, p);
            }
        } else {
            // si c’était juste la waiting-room et qu’elle est vide → supprime la salle
            if (wr.getParticipants().isEmpty()) {
                WaitingRoomManager.removeRoom(wr.getId());
            } else {
                wr.updateScoreboard();
            }
        }
    }

    private void endGameAndAnnounce(WaitingRoom wr, List<Player> winners, Player leaver) {
        GameManager gm = Blitz2v2.getInstance().getGameManager();

        // message chat
        String winColor = (winners == wr.getRedTeam()) ? "§cRouges" : "§9Bleus";
        Bukkit.broadcastMessage(winColor + " gagnent la partie !");

        // lance la fin de partie
        gm.endGame(wr, new ArrayList<>(winners));
    }
}
