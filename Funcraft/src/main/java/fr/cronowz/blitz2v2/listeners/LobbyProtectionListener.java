// src/main/java/fr/cronowz/blitz2v2/listeners/LobbyProtectionListener.java
package fr.cronowz.blitz2v2.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Protège totalement certains mondes (world_lobby, world et tous les clones world_waiting_* ) :
 * - empêche build/place/interact
 * - bloque tous les dommages (chute, mobs, PvP…)
 * - bloque la faim
 */
public class LobbyProtectionListener implements Listener {

    /** Vérifie si l'ItemStack est bien la boussole « Menu des parties ». */
    private boolean isCompass(ItemStack it) {
        if (it == null) return false;
        if (!it.hasItemMeta()) return false;
        if (it.getType() != Material.COMPASS) return false;
        return "§eMenu des parties".equals(it.getItemMeta().getDisplayName());
    }

    /** Vérifie si le joueur est dans un monde protégé. */
    private boolean isProtectedWorld(Player p) {
        String w = p.getWorld().getName();
        // Protège world_lobby, world, et tous les world_waiting_x
        return w.equals("world_lobby")
                || w.equals("world")
                || w.startsWith("world_waiting");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (isProtectedWorld(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (isProtectedWorld(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isProtectedWorld(p)) return;
        // Autorise uniquement le clic droit sur la boussole
        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
                && isCompass(e.getItem())) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent e) {
        String w = e.getBlock().getWorld().getName();
        if (w.equals("world_lobby") || w.equals("world") || w.startsWith("world_waiting")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (isProtectedWorld(p)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (isProtectedWorld(p)) {
                e.setCancelled(true);
                p.setFoodLevel(20);
            }
        }
    }
}
