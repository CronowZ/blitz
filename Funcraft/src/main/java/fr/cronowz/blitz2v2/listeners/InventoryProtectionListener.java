package fr.cronowz.blitz2v2.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class InventoryProtectionListener implements Listener {
    private boolean isCompass(ItemStack it) {
        return it != null
            && it.hasItemMeta()
            && it.getType() == Material.COMPASS
            && "§eMenu des parties".equals(it.getItemMeta().getDisplayName());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getRawSlot() == 0 && isCompass(e.getCurrentItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (isCompass(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
        }
    }
}
