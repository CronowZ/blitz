package fr.cronowz.blitz2v2.combat;

import fr.cronowz.blitz2v2.chat.TeamChatFormatter;
import fr.cronowz.blitz2v2.stats.KillStatsManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class CombatTrackerListener implements Listener {

    private static final long   COMBAT_TIMEOUT_MS   = 10_000L; // 10s
    private static final double ASSIST_MIN_FRACTION = 0.20;    // 20% des PV max

    private static class Hit {
        double totalDamage;
        long   lastTime;
    }

    // victim -> (attacker -> hit info)
    private final Map<UUID, Map<UUID, Hit>> combat = new HashMap<>();
    private final KillStatsManager stats;

    public CombatTrackerListener(KillStatsManager stats) {
        this.stats = stats;

        // Nettoyage périodique
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                Bukkit.getPluginManager().getPlugin("Blitz2v2"),
                () -> {
                    long now = System.currentTimeMillis();
                    for (Map<UUID, Hit> m : combat.values()) {
                        m.values().removeIf(h -> now - h.lastTime > COMBAT_TIMEOUT_MS);
                    }
                },
                20L * 30, 20L * 30);
    }

    private Player asPlayer(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Arrow) {
            Arrow a = (Arrow) damager;
            if (a.getShooter() instanceof Player) return (Player) a.getShooter();
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player victim = (Player) e.getEntity();
        Player attacker = asPlayer(e.getDamager());
        if (attacker == null) return;
        if (attacker.getUniqueId().equals(victim.getUniqueId())) return;

        combat.computeIfAbsent(victim.getUniqueId(), k -> new HashMap<>());
        Map<UUID, Hit> map = combat.get(victim.getUniqueId());
        Hit h = map.computeIfAbsent(attacker.getUniqueId(), k -> new Hit());
        h.totalDamage += e.getFinalDamage();
        h.lastTime    = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        Map<UUID, Hit> m = combat.get(p.getUniqueId());
        if (m == null || m.isEmpty()) return;
        long now = System.currentTimeMillis();
        // Si quit en combat, on compte comme une mort
        UUID killer = findKiller(m, now);
        if (killer != null) {
            handleDeath(p.getUniqueId(), killer, m, true);
        }
        combat.remove(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Map<UUID, Hit> m = combat.get(victim.getUniqueId());
        UUID killerId = null;

        if (m != null && !m.isEmpty()) {
            killerId = findKiller(m, System.currentTimeMillis());
        } else if (victim.getKiller() != null) {
            killerId = victim.getKiller().getUniqueId();
        }

        if (killerId != null) {
            handleDeath(victim.getUniqueId(), killerId, m, false);
        } else {
            // mort “naturelle”
            stats.addDeath(victim.getUniqueId());
        }

        combat.remove(victim.getUniqueId());
    }

    private UUID findKiller(Map<UUID, Hit> m, long now) {
        UUID best = null;
        double bestDmg = -1;
        for (Map.Entry<UUID, Hit> en : m.entrySet()) {
            Hit h = en.getValue();
            if (now - h.lastTime > COMBAT_TIMEOUT_MS) continue;
            if (h.totalDamage > bestDmg) {
                bestDmg = h.totalDamage;
                best = en.getKey();
            }
        }
        return best;
    }

    private void handleDeath(UUID victimId, UUID killerId, Map<UUID, Hit> contrib, boolean fromQuit) {
        stats.addDeath(victimId);

        Player victim = Bukkit.getPlayer(victimId);

        // Déterminer tous les joueurs méritant le kill (killer + gros dégâts)
        Set<UUID> killers = new LinkedHashSet<>();
        killers.add(killerId);

        if (contrib != null && victim != null) {
            double maxHP = Math.max(20.0, victim.getMaxHealth());
            for (Map.Entry<UUID, Hit> en : contrib.entrySet()) {
                UUID other = en.getKey();
                Hit h = en.getValue();
                if (other.equals(killerId)) continue;
                if (System.currentTimeMillis() - h.lastTime > COMBAT_TIMEOUT_MS) continue;
                if (h.totalDamage >= maxHP * ASSIST_MIN_FRACTION || h.totalDamage >= 1.0) {
                    killers.add(other);
                }
            }
        }

        // Stats + feedback pour chaque killer
        for (UUID id : killers) {
            stats.addKill(id);
            Player kp = Bukkit.getPlayer(id);
            if (kp != null) {
                try {
                    kp.playSound(kp.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                } catch (Throwable ignored) {}
                String txt = fromQuit ? "§e(l’a quitté en combat)" : "";
                kp.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent("§aTu as tué §f" + (victim != null ? victim.getName() : "un joueur") + " §a! " + txt));
            }
        }

        // Message global en jeu
        broadcastDeathMessage(killers, victim);
    }

    private void broadcastDeathMessage(Set<UUID> killers, Player victim) {
        if (victim == null) return;
        List<String> killerNames = new ArrayList<>();
        for (UUID id : killers) {
            Player p = Bukkit.getPlayer(id);
            killerNames.add(coloredName(p));
        }
        String victimName = coloredName(victim);

        String msg;
        if (killerNames.size() == 1) {
            msg = killerNames.get(0) + ChatColor.GRAY + " a tué " + victimName + ChatColor.GRAY + " !";
        } else if (killerNames.size() == 2) {
            msg = killerNames.get(0) + ChatColor.GRAY + " et " + killerNames.get(1)
                    + ChatColor.GRAY + " ont tué " + victimName + ChatColor.GRAY + " !";
        } else {
            String prefix = String.join(ChatColor.GRAY + ", ", killerNames.subList(0, killerNames.size() - 1));
            msg = prefix + ChatColor.GRAY + " et " + killerNames.get(killerNames.size() - 1)
                    + ChatColor.GRAY + " ont tué " + victimName + ChatColor.GRAY + " !";
        }

        for (Player p : victim.getWorld().getPlayers()) {
            p.sendMessage(msg);
        }
    }

    private String coloredName(Player p) {
        if (p == null) return ChatColor.GRAY + "un joueur";
        return TeamChatFormatter.teamColor(p) + p.getName();
    }
}
