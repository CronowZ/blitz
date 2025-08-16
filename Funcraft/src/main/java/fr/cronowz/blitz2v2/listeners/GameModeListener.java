// src/main/java/fr/cronowz/blitz2v2/listeners/GameModeListener.java
package fr.cronowz.blitz2v2.listeners;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.PartyManager;
import fr.cronowz.blitz2v2.manager.WaitingRoom;
import fr.cronowz.blitz2v2.manager.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GameModeListener implements Listener {

    private final Blitz2v2 plugin = Blitz2v2.getInstance();
    private final World gameWorld = Bukkit.getWorld(plugin.getCfg().getString("game.world"));

    private boolean isInGameWorld(Player p) {
        World w = p.getWorld();
        return w != null && w.equals(gameWorld);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!isInGameWorld(p)) return;

        // Définit la position de respawn + orientation selon l’équipe
        Location loc = gameWorld.getSpawnLocation().clone();
        WaitingRoom wr = PartyManager.getParty(p);
        if (wr != null) {
            if (wr.getRedTeam().contains(p)) {
                loc = GameManager.getRedSpawn().clone();
                loc.setYaw(-90f);
                loc.setPitch(0f);
            } else if (wr.getBlueTeam().contains(p)) {
                loc = GameManager.getBlueSpawn().clone();
                loc.setYaw(90f);
                loc.setPitch(0f);
            }
        }
        e.setRespawnLocation(loc);

        // Donne le kit + satiété 1 tick après pour être sûr que le joueur est bien dans le monde
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            giveKit(p, wr);
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.SATURATION,
                    3 * 20,
                    255,
                    false, false
            ));
        }, 1L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!isInGameWorld(p)) return;
        // Ne drop que les patates
        e.getDrops().removeIf(item -> item.getType() != Material.BAKED_POTATO);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getLocation().getWorld() == gameWorld) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        Material result = e.getRecipe().getResult().getType();
        if (result.name().contains("BOAT")) {
            e.setCancelled(true);
        }
    }

    private void giveKit(Player p, WaitingRoom wr) {
        p.getInventory().clear();

        // Armure cuir teintée selon l’équipe
        boolean isRed = (wr != null && wr.getRedTeam().contains(p));
        org.bukkit.Color color = isRed ? org.bukkit.Color.RED : org.bukkit.Color.BLUE;
        for (Material mat : new Material[]{
                Material.LEATHER_HELMET,
                Material.LEATHER_CHESTPLATE,
                Material.LEATHER_LEGGINGS,
                Material.LEATHER_BOOTS
        }) {
            ItemStack piece = new ItemStack(mat);
            LeatherArmorMeta meta = (LeatherArmorMeta) piece.getItemMeta();
            meta.setColor(color);
            piece.setItemMeta(meta);
            switch (mat) {
                case LEATHER_HELMET:
                    p.getInventory().setHelmet(piece);
                    break;
                case LEATHER_CHESTPLATE:
                    p.getInventory().setChestplate(piece);
                    break;
                case LEATHER_LEGGINGS:
                    p.getInventory().setLeggings(piece);
                    break;
                case LEATHER_BOOTS:
                    p.getInventory().setBoots(piece);
                    break;
                default:
                    break;
            }
        }

        // Kit de base
        p.getInventory().setItem(0, new ItemStack(Material.WOOD_SWORD));
        p.getInventory().setItem(1, new ItemStack(Material.WOOD_AXE));

        int potatoCount = plugin.getCfg().getInt("kit.potatoes", 12);
        p.getInventory().setItem(2, new ItemStack(Material.BAKED_POTATO, potatoCount));

        // Pioche avec durabilité réduite à la valeur de config
        int remainingDur = plugin.getCfg().getInt("kit.pickaxe-remaining-durability", 20);
        ItemStack pick = new ItemStack(Material.STONE_PICKAXE);
        short maxDur   = pick.getType().getMaxDurability();
        pick.setDurability((short) (maxDur - remainingDur));
        p.getInventory().setItem(3, pick);
    }
}
