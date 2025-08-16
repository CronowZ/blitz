// src/main/java/fr/cronowz/blitz2v2/listeners/CombatListener.java
package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.PartyManager;
import fr.cronowz.blitz2v2.game.GameSession;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.projectiles.ProjectileSource;

public class CombatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;

        Player victim   = (Player) e.getEntity();
        Player attacker = getAttacker(e.getDamager());
        if (attacker == null) return;

        // --- Important pour les parties simultanées ---
        // On n'applique la logique que si les deux joueurs sont
        // dans la même GameSession (même partie).
        GameSession sessVictim  = Blitz2v2.getInstance().getGameManager().getSessionForPlayer(victim);
        if (sessVictim == null) return; // la victime n’est pas en partie

        GameSession sessAttacker = Blitz2v2.getInstance().getGameManager().getSessionForPlayer(attacker);
        if (sessAttacker == null || sessAttacker != sessVictim) return; // pas la même partie

        // Anti-friendly fire (même équipe)
        if (sameTeam(attacker, victim)) {
            e.setCancelled(true);
            return;
        }

        // Bonus dégâts à la hache en bois (règle du mode)
        ItemStack hand = attacker.getInventory().getItemInMainHand();
        if (hand != null && hand.getType() == Material.WOOD_AXE) {
            e.setDamage(2.0D);
        }
    }

    private Player getAttacker(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile) {
            ProjectileSource src = ((Projectile) damager).getShooter();
            if (src instanceof Player) return (Player) src;
        }
        return null;
    }

    private boolean sameTeam(Player a, Player b) {
        WaitingRoom wrA = PartyManager.getParty(a);
        WaitingRoom wrB = PartyManager.getParty(b);
        if (wrA == null || wrB == null || wrA.getId() != wrB.getId()) return false;
        return (wrA.getRedTeam().contains(a)  && wrA.getRedTeam().contains(b))
                || (wrA.getBlueTeam().contains(a) && wrA.getBlueTeam().contains(b));
    }
}
