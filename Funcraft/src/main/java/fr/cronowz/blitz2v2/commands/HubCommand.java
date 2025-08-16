// src/main/java/fr/cronowz/blitz2v2/commands/HubCommand.java
package fr.cronowz.blitz2v2.commands;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.PartyManager;
import fr.cronowz.blitz2v2.manager.GameManager;
import fr.cronowz.blitz2v2.manager.WaitingRoomManager;
import fr.cronowz.blitz2v2.listeners.JoinListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HubCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender,
                             Command cmd,
                             String label,
                             String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        // 0) Sortir proprement de toute session de jeu en cours
        GameManager gm = Blitz2v2.getInstance().getGameManager();
        gm.handlePlayerExit(player);

        // 1) Quitte toutes les parties en cours
        PartyManager.leaveAll(player);

        // 2) Quitte la waiting-room si besoin
        WaitingRoomManager.leaveWaitingRoom(player);

        // 3) Retire le sélecteur d’équipe (laine)
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null
                    && item.getType() == Material.WOOL
                    && item.hasItemMeta()
                    && "§eSélectionner équipe".equals(item.getItemMeta().getDisplayName())) {
                player.getInventory().remove(item);
            }
        }

        // 4) Téléport au lobby
        World lobbyWorld = Bukkit.getWorld("world_lobby");
        if (lobbyWorld == null) {
            player.sendMessage("§cMonde world_lobby introuvable !");
            return true;
        }
        player.teleport(new Location(lobbyWorld, 545, 50, 415));

        // 5) Clear l’inventaire et donne la boussole
        player.getInventory().clear();
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName("§eMenu des parties");
        compass.setItemMeta(meta);
        player.getInventory().setItem(0, compass);

        // 6) Scoreboard Lobby via JoinListener
        JoinListener.showLobbyBoard(player);

        player.sendMessage("§a● Téléporté au lobby !");
        return true;
    }
}
