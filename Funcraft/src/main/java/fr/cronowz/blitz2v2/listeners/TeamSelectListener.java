// src/main/java/fr/cronowz/blitz2v2/listeners/TeamSelectListener.java
package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.PartyManager;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class TeamSelectListener implements Listener {
    private static final String TITLE         = "§bChoix de l'équipe";
    private static final int    SLOT_SELECTOR = 7;
    private static final int    SLOT_RED      = 3;
    private static final int    SLOT_RANDOM   = 4;
    private static final int    SLOT_BLUE     = 5;

    /** Donne le sélecteur de team (laine “§eSélectionner équipe”) au slot 7. */
    public static void giveSelector(Player p) {
        if (p == null) return;
        ItemStack selector = new ItemStack(Material.WOOL, 1, (short)0);
        ItemMeta meta = selector.getItemMeta();
        meta.setDisplayName("§eSélectionner équipe");
        selector.setItemMeta(meta);
        p.getInventory().setItem(SLOT_SELECTOR, selector);
        p.updateInventory();
    }

    @EventHandler
    public void onWoolClick(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack it = e.getItem();
        if (it == null || it.getType() != Material.WOOL || !it.hasItemMeta()) return;
        ItemMeta im = it.getItemMeta();
        if (im == null || !"§eSélectionner équipe".equals(im.getDisplayName())) return;

        e.setCancelled(true);
        openMenu(e.getPlayer());
    }

    private void openMenu(Player p) {
        WaitingRoom wr = PartyManager.getParty(p);

        List<Player> redPlayers    = (wr != null) ? wr.getRedTeam()  : List.of();
        List<Player> bluePlayers   = (wr != null) ? wr.getBlueTeam() : List.of();
        List<Player> randomPlayers;
        if (wr != null) {
            randomPlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(pl -> wr.getParticipants().contains(pl))
                    .filter(pl -> !redPlayers.contains(pl) && !bluePlayers.contains(pl))
                    .collect(Collectors.toList());
        } else {
            randomPlayers = List.of();
        }

        Inventory inv = Bukkit.createInventory(null, 9, TITLE);

        // Équipe Rouge
        ItemStack red = new ItemStack(Material.WOOL, 1, (short)14);
        ItemMeta rm = red.getItemMeta();
        rm.setDisplayName("§cÉquipe Rouge");
        rm.setLore(loreFromList("§7Joueurs :", "§c", redPlayers));
        red.setItemMeta(rm);
        inv.setItem(SLOT_RED, red);

        // Aléatoire
        ItemStack rnd = new ItemStack(Material.WOOL, 1, (short)7);
        ItemMeta rdm = rnd.getItemMeta();
        rdm.setDisplayName("§7Aléatoire");
        rdm.setLore(loreFromList("§7Joueurs aléatoires :", "§7", randomPlayers));
        rnd.setItemMeta(rdm);
        inv.setItem(SLOT_RANDOM, rnd);

        // Équipe Bleue
        ItemStack blue = new ItemStack(Material.WOOL, 1, (short)11);
        ItemMeta bm = blue.getItemMeta();
        bm.setDisplayName("§9Équipe Bleue");
        bm.setLore(loreFromList("§7Joueurs :", "§9", bluePlayers));
        blue.setItemMeta(bm);
        inv.setItem(SLOT_BLUE, blue);

        p.openInventory(inv);
    }

    private List<String> loreFromList(String header, String color, List<Player> players) {
        if (players == null || players.isEmpty()) {
            return List.of("§7Aucun joueur");
        }
        String names = players.stream().map(Player::getName).collect(Collectors.joining(", " + color));
        return List.of(header, color + names);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta() || it.getItemMeta().getDisplayName() == null) return;

        WaitingRoom wr = PartyManager.getParty(p);
        if (wr == null) {
            p.closeInventory();
            p.sendMessage("§cTu n'es plus dans une salle d'attente.");
            return;
        }

        String name = it.getItemMeta().getDisplayName();

        // Choix Rouge
        if ("§cÉquipe Rouge".equals(name)) {
            if (wr.getRedTeam().contains(p)) {
                p.sendMessage("§cTu es déjà dans l'équipe Rouge.");
            } else if (wr.getRedTeam().size() >= 2) {
                p.sendMessage("§cL'équipe Rouge est déjà complète !");
            } else {
                wr.assignTeam(p, WaitingRoom.Team.RED);
                p.sendMessage("§cTu as rejoint l'équipe Rouge !");
                updateSelector(p, (short)14);
                wr.updateScoreboard();
            }
        }
        // Choix Aléatoire
        else if ("§7Aléatoire".equals(name)) {
            wr.assignTeam(p, WaitingRoom.Team.RANDOM);
            p.sendMessage("§7Tu seras placé·e aléatoirement !");
            updateSelector(p, (short)7);
            wr.updateScoreboard();
        }
        // Choix Bleu
        else if ("§9Équipe Bleue".equals(name)) {
            if (wr.getBlueTeam().contains(p)) {
                p.sendMessage("§9Tu es déjà dans l'équipe Bleue.");
            } else if (wr.getBlueTeam().size() >= 2) {
                p.sendMessage("§cL'équipe Bleue est déjà complète !");
            } else {
                wr.assignTeam(p, WaitingRoom.Team.BLUE);
                p.sendMessage("§9Tu as rejoint l'équipe Bleue !");
                updateSelector(p, (short)11);
                wr.updateScoreboard();
            }
        }

        p.closeInventory();
    }

    /** Met à jour la laine en slot 7 selon la couleur choisie */
    private void updateSelector(Player p, short color) {
        ItemStack selector = new ItemStack(Material.WOOL, 1, color);
        ItemMeta sm = selector.getItemMeta();
        sm.setDisplayName("§eSélectionner équipe");
        selector.setItemMeta(sm);
        p.getInventory().setItem(SLOT_SELECTOR, selector);
        p.updateInventory();
    }
}
