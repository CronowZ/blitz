package fr.cronowz.blitz2v2.protect;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ProtectWandListener implements Listener {

    private final ProtectionManager pm;

    public ProtectWandListener(ProtectionManager pm) {
        this.pm = pm;
    }

    private boolean isWand(ItemStack it) {
        return it != null && it.getType() == Material.WOOD_AXE;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!p.hasPermission("blitz2v2.protect.admin")) return;
        if (!isWand(p.getItemInHand())) return;

        Action a = e.getAction();
        Block b = e.getClickedBlock();
        if (b == null) return;

        UUID id = p.getUniqueId();
        if (a == Action.LEFT_CLICK_BLOCK) {
            pm.setPos1(id, b.getLocation());
            p.sendMessage(ChatColor.YELLOW + "[Protect] pos1 = " + fmt(b));
            playClick(p);
            e.setCancelled(true);
        } else if (a == Action.RIGHT_CLICK_BLOCK) {
            pm.setPos2(id, b.getLocation());
            p.sendMessage(ChatColor.YELLOW + "[Protect] pos2 = " + fmt(b));
            playClick(p);
            e.setCancelled(true);
        }
    }

    private String fmt(Block b) {
        return ChatColor.GRAY + "(" + b.getX() + ", " + b.getY() + ", " + b.getZ() + ") "
                + ChatColor.DARK_GRAY + "in " + ChatColor.GRAY + b.getWorld().getName();
    }

    /** Essaie plusieurs noms de sons selon la version (évite les constants absents en 1.9.4) */
    private void playClick(Player p) {
        String[] candidates = {
                "UI_BUTTON_CLICK",
                "BLOCK_WOOD_BUTTON_CLICK_ON",
                "BLOCK_LEVER_CLICK",
                "CLICK" // ancien nom (1.8)
        };
        for (String name : candidates) {
            try {
                Sound s = Sound.valueOf(name);
                p.playSound(p.getLocation(), s, 1f, 1f);
                return;
            } catch (IllegalArgumentException ignored) {
                // pas dispo dans cette version, on tente le suivant
            }
        }
        // si aucun son n'est dispo, on ne fait rien
    }
}
