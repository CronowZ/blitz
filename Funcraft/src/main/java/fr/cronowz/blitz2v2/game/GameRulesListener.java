// src/main/java/fr/cronowz/blitz2v2/game/GameRulesListener.java
package fr.cronowz.blitz2v2.game;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.PartyManager;
import fr.cronowz.blitz2v2.manager.GameManager;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import fr.cronowz.blitz2v2.game.KitService.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;          // ← ajouté
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;

public class GameRulesListener implements Listener {

    private final JavaPlugin plugin;
    private final KitService kitService;
    /** Préfixe du monde de jeu (ex: "world_game") */
    private final String gameWorldBase;
    /** Durée des buffs en ticks (3 s = 60 ticks) */
    private static final int EFFECT_DURATION = 3 * 20;

    public GameRulesListener(JavaPlugin plugin, KitService kitService, String gameWorldBase) {
        this.plugin = plugin;
        this.kitService = kitService;
        this.gameWorldBase = gameWorldBase;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (isGameWorld(p.getWorld())) {
            setupPlayerInGame(p);
        }
    }

    /** Mort instantanée dans le vide en jeu (pas de grignotage de PV) */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVoidDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.VOID) return;

        Player p = (Player) e.getEntity();
        if (!isGameWorld(p.getWorld())) return; // on laisse le lobby/waiting tranquille

        e.setCancelled(true); // empêche les dégâts progressifs
        p.setHealth(0.0);     // tue instantanément → onDeath + respawn forcé s'appliquent
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!isGameWorld(p.getWorld())) return;

        // 1) Ne drop que les patates du kit de base
        Iterator<ItemStack> it = e.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack item = it.next();
            if (kitService.isBaseItem(item) && !kitService.isBasePotato(item)) {
                it.remove();
            }
        }

        // 2) Respawn forcé 1 tick plus tard pour éviter le menu de mort
        Bukkit.getScheduler().runTaskLater(plugin, () -> p.spigot().respawn(), 1L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();

        // S’applique uniquement si le joueur est en partie OU si la loc de respawn est dans un monde de game
        boolean inSession = Blitz2v2.getInstance().getGameManager().getSessionForPlayer(p) != null;
        if (!inSession && !isGameWorld(e.getRespawnLocation().getWorld())) return;

        Location loc = e.getRespawnLocation();

        WaitingRoom wr = PartyManager.getParty(p);
        if (wr != null) {
            // on clone pour ne pas modifier le spawn interne
            if (wr.getRedTeam().contains(p)) {
                loc = GameManager.getRedSpawn().clone();
            } else if (wr.getBlueTeam().contains(p)) {
                loc = GameManager.getBlueSpawn().clone();
            }
        }

        // applique directement la location (yaw/pitch inclus)
        e.setRespawnLocation(loc);

        // 1 tick plus tard : kit + effets
        Bukkit.getScheduler().runTask(plugin, () -> {
            TeamColor color = getTeamColor(p);
            if (color != null) {
                kitService.giveKit(p, color);
                applySpawnEffects(p);
            }
        });
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent e) {
        if (isGameWorld(e.getLocation().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (e.getInventory().getResult() == null) return;
        if (!(e.getView().getPlayer() instanceof Player)) return;
        Player p = (Player) e.getView().getPlayer();
        if (!isGameWorld(p.getWorld())) return;

        Material result = e.getInventory().getResult().getType();
        if (result == Material.BOAT || result.name().endsWith("_BOAT")) {
            e.getInventory().setResult(null);
        }
    }

    // ──────────────────────────────────
    // Helpers
    // ──────────────────────────────────

    private void setupPlayerInGame(Player p) {
        TeamColor color = getTeamColor(p);
        if (color == null) return;

        kitService.giveKit(p, color);
        setTeamSpawnPointIfNeeded(p);
        applySpawnEffects(p);
    }

    private void applySpawnEffects(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, EFFECT_DURATION, 255, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, EFFECT_DURATION, 1, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, EFFECT_DURATION, 1, true));
    }

    private void setTeamSpawnPointIfNeeded(Player p) {
        WaitingRoom wr = PartyManager.getParty(p);
        if (wr == null) return;

        TeamColor color = getTeamColor(p);
        Location spawn = (color == TeamColor.RED)
                ? GameManager.getRedSpawn()
                : GameManager.getBlueSpawn();
        // Force la position de respawn immédiatement
        p.setBedSpawnLocation(spawn, true);
    }

    private TeamColor getTeamColor(Player p) {
        WaitingRoom wr = PartyManager.getParty(p);
        if (wr == null) return null;
        if (wr.getRedTeam().contains(p))  return TeamColor.RED;
        if (wr.getBlueTeam().contains(p)) return TeamColor.BLUE;
        return null;
    }

    private boolean isGameWorld(World w) {
        if (w == null) return false;
        String n = w.getName().toLowerCase();
        String base = gameWorldBase.toLowerCase();
        // match "world_game" et "world_game_*"
        return n.equals(base) || n.startsWith(base + "_");
    }
}
