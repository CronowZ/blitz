// src/main/java/fr/cronowz/blitz2v2/protect/WandListener.java
package fr.cronowz.blitz2v2.protect;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WandListener implements Listener {

    private final SelectionManager sm;

    public WandListener(SelectionManager sm) {
        this.sm = sm;
    }

    private boolean isWand(ItemStack it) {
        return it != null && it.getType() == Material.STICK;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!p.hasPermission("blitz2v2.protection.admin")) return;

        if (!isWand(p.getItemInHand())) return;

        Action a = e.getAction();
        Block  b = e.getClickedBlock();
        if (b == null) return;

        if (a == Action.LEFT_CLICK_BLOCK) {
            sm.setPos1(p.getUniqueId(), b.getLocation());
            p.sendMessage(ChatColor.YELLOW + "[Prot] " + ChatColor.WHITE +
                    "Pos1 = (" + b.getX() + ", " + b.getY() + ", " + b.getZ() + ") " +
                    ChatColor.DARK_GRAY + "[" + b.getWorld().getName() + "]");
            e.setCancelled(true);
        } else if (a == Action.RIGHT_CLICK_BLOCK) {
            sm.setPos2(p.getUniqueId(), b.getLocation());
            p.sendMessage(ChatColor.YELLOW + "[Prot] " + ChatColor.WHITE +
                    "Pos2 = (" + b.getX() + ", " + b.getY() + ", " + b.getZ() + ") " +
                    ChatColor.DARK_GRAY + "[" + b.getWorld().getName() + "]");
            e.setCancelled(true);
        }
    }
}
