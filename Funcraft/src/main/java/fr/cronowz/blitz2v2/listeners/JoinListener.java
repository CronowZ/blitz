// src/main/java/fr/cronowz/blitz2v2/listeners/JoinListener.java
package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.Blitz2v2;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.*;

import java.util.Arrays;

public class JoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        FileConfiguration cfg = Blitz2v2.getInstance().getCfg();

        // 0) reset safe (sortir d’un éventuel état “partie” résiduel)
        try {
            if (Bukkit.getScoreboardManager() != null) {
                p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        } catch (Throwable ignored) {}
        p.setGameMode(GameMode.SURVIVAL);
        p.setInvulnerable(false);
        p.setAllowFlight(false);
        p.setFlying(false);
        p.setFallDistance(0);
        p.setFireTicks(0);
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.setHealth(Math.max(0.1D, p.getMaxHealth()));
        for (PotionEffect pe : p.getActivePotionEffects()) {
            p.removePotionEffect(pe.getType());
        }

        // 1) TP lobby depuis la config (avec fallback propre)
        Location lobbySpawn = Blitz2v2.getInstance().getLobbySpawn();
        if (lobbySpawn == null) {
            String worldName = cfg.getString("lobby.world", "world_lobby");
            double x = cfg.getDouble("lobby.x", 545);
            double y = cfg.getDouble("lobby.y", 50);
            double z = cfg.getDouble("lobby.z", 415);
            World lobby = Bukkit.getWorld(worldName);
            if (lobby == null) {
                Blitz2v2.getInstance().getLogger().severe("Le monde '" + worldName + "' est introuvable !");
            } else {
                lobbySpawn = new Location(lobby, x, y, z);
            }
        }
        if (lobbySpawn != null) {
            p.teleport(lobbySpawn);
        }

        // 2) Inventaire : clear total + purge sélecteur d’équipe résiduel
        p.getInventory().clear();
        p.getInventory().setArmorContents(new ItemStack[4]);
        cleanupTeamSelector(p);

        // 3) Donne la boussole au slot 0
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName("§eMenu des parties");
        compass.setItemMeta(meta);
        p.getInventory().setItem(0, compass);
        p.updateInventory();

        // 4) Scoreboard Lobby après un léger délai (laisse le client respirer)
        Bukkit.getScheduler().runTaskLater(
                Blitz2v2.getInstance(),
                () -> showLobbyBoard(p),
                20L
        );
    }

    /** Affiche le scoreboard du lobby pour un joueur. */
    public static void showLobbyBoard(Player p) {
        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;

        Scoreboard board = mgr.getNewScoreboard();
        Objective obj = board.registerNewObjective("lobby", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName("§6Plexymc.org");

        int line = 6;
        obj.getScore(" ").setScore(line--);
        obj.getScore("§fMode: §eLobby").setScore(line--);
        obj.getScore("§fEn ligne: §a" + Bukkit.getOnlinePlayers().size()
                + "§7/" + Bukkit.getMaxPlayers()).setScore(line--);
        obj.getScore(" ").setScore(line--);
        obj.getScore(" ").setScore(line--);
        obj.getScore(" ").setScore(line--);
        obj.getScore("§fJoueur: §a" + p.getName()).setScore(line--);
        obj.getScore("§fVisitez: §bplexymc.org").setScore(line--);
        obj.getScore("  ").setScore(line);

        p.setScoreboard(board);
    }

    /** Supprime toute laine ‘§eSélectionner équipe’ qui aurait survécu à un retour lobby. */
    private void cleanupTeamSelector(Player p) {
        Arrays.stream(p.getInventory().getContents())
                .filter(it -> it != null
                        && it.getType() == Material.WOOL
                        && it.hasItemMeta()
                        && "§eSélectionner équipe".equals(it.getItemMeta().getDisplayName()))
                .forEach(it -> p.getInventory().remove(it));
    }
}
