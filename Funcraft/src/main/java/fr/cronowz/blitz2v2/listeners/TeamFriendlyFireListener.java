// src/main/java/fr/cronowz/blitz2v2/listeners/TeamFriendlyFireListener.java
package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.manager.GameManager;
import fr.cronowz.blitz2v2.game.GameSession;
import fr.cronowz.blitz2v2.game.KitService.TeamColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class TeamFriendlyFireListener implements Listener {

    private final GameManager gm = Blitz2v2.getInstance().getGameManager();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Victime doit être un joueur
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();

        // Attaquant: joueur direct ou tireur d’un projectile
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) return;

        // Les deux doivent être dans la même session de jeu
        GameSession sessVictim  = gm.getSessionForPlayer(victim);
        GameSession sessAttacker = gm.getSessionForPlayer(attacker);
        if (sessVictim == null || sessVictim != sessAttacker) return;

        // Même équipe -> pas de dégâts
        TeamColor tv = sessVictim.getTeamColor(victim);
        TeamColor ta = sessVictim.getTeamColor(attacker);
        if (tv != null && tv == ta) {
            event.setCancelled(true);
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile) {
            ProjectileSource src = ((Projectile) damager).getShooter();
            if (src instanceof Player) return (Player) src;
        }
        return null;
    }
}
