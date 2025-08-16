// src/main/java/fr/cronowz/blitz2v2/listeners/CompassMenuListener.java
package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.manager.WaitingRoomManager;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CompassMenuListener implements Listener {
    private static final String GAME_MENU_TITLE = "§eMenu des jeux";
    private static final String MODE_MENU_TITLE = "§eBlitz 2vs2";
    private static final String LIST_MENU_TITLE = "§6Parties Blitz2v2";
    private static final short  CLAY_DATA       = 14; // rouge
    private static final int    SLOT_CENTER     = 4;

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        ItemStack held = e.getItem();
        if (held == null || held.getType() != Material.COMPASS) return;

        e.setCancelled(true);
        Player p = e.getPlayer();

        // 1) Menu des jeux
        Inventory menu = Bukkit.createInventory(null, 9, GAME_MENU_TITLE);
        ItemStack clay = new ItemStack(Material.STAINED_CLAY, 1, CLAY_DATA);
        ItemMeta meta = clay.getItemMeta();
        meta.setDisplayName("§cBlitz");
        clay.setItemMeta(meta);
        menu.setItem(SLOT_CENTER, clay);

        p.openInventory(menu);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (!title.equals(GAME_MENU_TITLE)
                && !title.equals(MODE_MENU_TITLE)
                && !title.equals(LIST_MENU_TITLE)) return;

        e.setCancelled(true);
        Player p = (Player)e.getWhoClicked();
        int slot = e.getRawSlot();

        // --- Étape 1 : Menu des jeux ---
        if (title.equals(GAME_MENU_TITLE)) {
            if (slot != SLOT_CENTER) return;
            // Passe au sous-menu Blitz 2vs2
            Inventory modeMenu = Bukkit.createInventory(null, 9, MODE_MENU_TITLE);
            ItemStack clay2 = new ItemStack(Material.STAINED_CLAY, 1, CLAY_DATA);
            ItemMeta m2 = clay2.getItemMeta();
            m2.setDisplayName("§cBlitz 2vs2");
            clay2.setItemMeta(m2);
            modeMenu.setItem(SLOT_CENTER, clay2);
            p.openInventory(modeMenu);
        }

        // --- Étape 2 : Sous-menu Blitz 2vs2 ---
        else if (title.equals(MODE_MENU_TITLE)) {
            if (slot != SLOT_CENTER) return;
            if (e.getClick().isLeftClick()) {
                WaitingRoomManager.autoJoin(p);
                p.closeInventory();
            } else if (e.getClick().isRightClick()) {
                // Ouvre la liste des parties
                Inventory list = Bukkit.createInventory(null, 9, LIST_MENU_TITLE);
                List<WaitingRoom> rooms = WaitingRoomManager.getRooms();
                int maxPlayers = Blitz2v2.getInstance().getCfg().getInt("countdown.max-players", 4);

                for (int i = 0; i < rooms.size() && i < list.getSize(); i++) {
                    WaitingRoom wr = rooms.get(i);
                    int count = wr.getParticipants().size();
                    boolean started = wr.isStarted();
                    boolean full = wr.isFull();

                    short color;
                    String statusText;
                    if (started) {
                        color = 1;            // orange
                        statusText = "§cEn cours";
                    } else if (full) {
                        color = 14;           // rouge
                        statusText = "§4Pleine";
                    } else {
                        color = 5;            // lime
                        statusText = "§aLibre";
                    }

                    ItemStack roomItem = new ItemStack(Material.STAINED_CLAY, 1, color);
                    ItemMeta m = roomItem.getItemMeta();
                    m.setDisplayName("§eSalle #" + wr.getId());
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Joueurs: " + count + " / " + maxPlayers);
                    lore.add(statusText);
                    m.setLore(lore);
                    roomItem.setItemMeta(m);

                    list.setItem(i, roomItem);
                }
                p.openInventory(list);
            }
        }

        // --- Étape 3 : Liste des parties ---
        else if (title.equals(LIST_MENU_TITLE)) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            String name = clicked.getItemMeta().getDisplayName();
            if (!name.startsWith("§eSalle #")) return;

            try {
                int id = Integer.parseInt(name.substring(name.indexOf('#') + 1));
                WaitingRoomManager.joinRoom(id, p);
            } catch (NumberFormatException ignored) { }
            p.closeInventory();
        }
    }
}
