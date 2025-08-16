// src/main/java/fr/cronowz/blitz2v2/protect/ProtectionListener.java
package fr.cronowz.blitz2v2.protect;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;
import java.util.List;

public class ProtectionListener implements Listener {

    private final ProtectionManager pm;

    public ProtectionListener(ProtectionManager pm) {
        this.pm = pm;
    }

    private boolean hasBypass(Player p) {
        return p != null && p.hasPermission("blitz2v2.protection.bypass");
    }

    private boolean isChest(Block b) {
        if (b == null) return false;
        Material t = b.getType();
        return t == Material.CHEST || t == Material.TRAPPED_CHEST;
    }

    // Casser
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();

        // Coffres incassables
        if (pm.isChestsUnbreakable() && isChest(b) && pm.isChestProtectedWorld(b.getWorld().getName())) {
            if (!hasBypass(p)) {
                e.setCancelled(true);
                return;
            }
        }
        // Régions
        if (pm.isProtected(b.getLocation()) && !hasBypass(p)) {
            e.setCancelled(true);
        }
    }

    // Poser
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (pm.isProtected(e.getBlockPlaced().getLocation()) && !hasBypass(p)) {
            e.setCancelled(true);
        }
    }

    // Explosions entités
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        sanitizeExplosionList(e.blockList(), e.getEntity());
    }

    // Explosions blocs
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        sanitizeExplosionList(e.blockList(), null);
    }

    private void sanitizeExplosionList(List<Block> list, Entity cause) {
        Iterator<Block> it = list.iterator();
        while (it.hasNext()) {
            Block b = it.next();

            if (pm.isChestsUnbreakable() && isChest(b) && pm.isChestProtectedWorld(b.getWorld().getName())) {
                it.remove();
                continue;
            }
            if (pm.isProtected(b.getLocation())) {
                it.remove();
            }
        }
    }

    // Pistons
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block moved : e.getBlocks()) {
            Block dest = moved.getRelative(e.getDirection());
            if (pm.isProtected(moved.getLocation()) || pm.isProtected(dest.getLocation())) {
                e.setCancelled(true);
                return;
            }
        }
        Block head = e.getBlock().getRelative(e.getDirection());
        if (pm.isProtected(head.getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (!e.isSticky()) return;
        for (Block moved : e.getBlocks()) {
            Block dest = moved.getRelative(e.getDirection());
            if (pm.isProtected(moved.getLocation()) || pm.isProtected(dest.getLocation())) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
