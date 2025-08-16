// src/main/java/fr/cronowz/blitz2v2/listeners/SubMenuListener.java
package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import fr.cronowz.blitz2v2.manager.WaitingRoomManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SubMenuListener implements Listener {

    private static final String TITLE = "Choix du jeu";

    public static void openPartyMenu(Player p) {
        FileConfiguration cfg = Blitz2v2.getInstance().getCfg();
        int maxRooms = cfg.getInt("waiting.max-rooms", 5);

        // Si aucune salle : on en crée une
        if (WaitingRoomManager.getRooms().isEmpty()) {
            WaitingRoomManager.getOrCreateRoom();
        }

        List<WaitingRoom> rooms = WaitingRoomManager.getRooms();
        int count = Math.min(maxRooms, rooms.size());
        int rows  = (count + 8) / 9;
        Inventory inv = Bukkit.createInventory(null, rows * 9, TITLE);

        for (int i = 0; i < rooms.size() && i < maxRooms; i++) {
            WaitingRoom wr = rooms.get(i);
            // WOOL + data: 14=rouge, 5=vert
            short data = (short)(wr.isFull() ? 14 : 5);
            org.bukkit.inventory.ItemStack wool =
                    new org.bukkit.inventory.ItemStack(Material.WOOL, 1, data);
            ItemMeta meta = wool.getItemMeta();
            meta.setDisplayName("Salle #" + wr.getId());
            meta.setLore(List.of(
                    wr.getParticipants().size() + "/" +
                            cfg.getInt("countdown.max-players", 4) + " joueurs",
                    wr.isStarted() ? "Départ imminent" : "En attente..."
            ));
            wool.setItemMeta(meta);
            inv.setItem(i, wool);
        }

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        org.bukkit.inventory.ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();
        if (!name.startsWith("Salle #")) return;
        int id;
        try {
            id = Integer.parseInt(name.substring(7));
        } catch (NumberFormatException ex) {
            return;
        }

        if (WaitingRoomManager.joinRoom(id, p)) {
            p.sendMessage("§aTu as rejoint la salle d'attente #" + id + " !");
            p.closeInventory();
        } else {
            p.sendMessage("§cImpossible de rejoindre la salle #" + id + ".");
        }
    }
}
