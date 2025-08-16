// src/main/java/fr/cronowz/blitz2v2/game/KitService.java
package fr.cronowz.blitz2v2.game;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.List;

import static fr.cronowz.blitz2v2.game.KitMarkers.*;

public class KitService {

    private final int potatoAmount;
    private final int pickRemainingDurability;

    public KitService(int potatoAmount, int pickRemainingDurability) {
        this.potatoAmount = potatoAmount;
        this.pickRemainingDurability = pickRemainingDurability;
    }

    public void giveKit(Player p, TeamColor team) {
        // on vide avant de redonner
        p.getInventory().clear();

        // armures colorées
        p.getInventory().setHelmet(coloredArmor(Material.LEATHER_HELMET, team));
        p.getInventory().setChestplate(coloredArmor(Material.LEATHER_CHESTPLATE, team));
        p.getInventory().setLeggings(coloredArmor(Material.LEATHER_LEGGINGS, team));
        p.getInventory().setBoots(coloredArmor(Material.LEATHER_BOOTS, team));

        // outils / consommables
        p.getInventory().setItem(0, markBase(new ItemStack(Material.WOOD_SWORD)));
        p.getInventory().setItem(1, markBase(new ItemStack(Material.WOOD_AXE)));
        p.getInventory().setItem(2, markPotato(new ItemStack(Material.BAKED_POTATO, potatoAmount)));

        ItemStack pick = new ItemStack(Material.STONE_PICKAXE);
        short max = pick.getType().getMaxDurability();
        short damage = (short) Math.max(0, max - pickRemainingDurability);
        pick.setDurability(damage);
        p.getInventory().setItem(3, markBase(pick));

        p.updateInventory();
    }

    public boolean isBaseItem(ItemStack it) {
        return hasLore(it, LORE_KIT);
    }

    public boolean isBasePotato(ItemStack it) {
        return hasLore(it, LORE_KIT_POTATO);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private ItemStack coloredArmor(Material mat, TeamColor team) {
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(team == TeamColor.RED ? Color.RED : Color.BLUE);
        addLore(meta, LORE_KIT);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack markBase(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        addLore(meta, LORE_KIT);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack markPotato(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        addLore(meta, LORE_KIT);
        addLore(meta, LORE_KIT_POTATO);
        it.setItemMeta(meta);
        return it;
    }

    private void addLore(ItemMeta meta, String tag) {
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (!lore.contains(tag)) lore.add(tag);
        meta.setLore(lore);
    }

    private boolean hasLore(ItemStack it, String tag) {
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasLore()) return false;
        return it.getItemMeta().getLore().contains(tag);
    }

    public enum TeamColor { RED, BLUE }
}
