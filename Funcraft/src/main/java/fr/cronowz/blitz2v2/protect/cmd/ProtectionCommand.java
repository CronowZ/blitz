// src/main/java/fr/cronowz/blitz2v2/protect/cmd/ProtectionCommand.java
package fr.cronowz.blitz2v2.protect.cmd;

import fr.cronowz.blitz2v2.Blitz2v2;
import fr.cronowz.blitz2v2.protect.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class ProtectionCommand implements CommandExecutor, TabCompleter {

    private final Blitz2v2 plugin;
    private final ProtectionManager pm;
    private final SelectionManager  sm;

    public ProtectionCommand(Blitz2v2 plugin, ProtectionManager pm, SelectionManager sm) {
        this.plugin = plugin;
        this.pm = pm;
        this.sm = sm;
    }

    private void msg(CommandSender s, String m) { s.sendMessage(ChatColor.YELLOW + "[Prot] " + ChatColor.WHITE + m); }
    private boolean adminOnly(CommandSender s) {
        if (!(s instanceof Player)) { msg(s, "Commande en jeu uniquement."); return false; }
        if (!s.hasPermission("blitz2v2.protection.admin")) { msg(s, "Permission manquante."); return false; }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            msg(sender, "Sous-commandes : wand | define <id> [base] | list | reload | test");
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload": {
                if (!sender.hasPermission("blitz2v2.protection.admin")) { msg(sender, "Permission manquante."); return true; }
                plugin.reloadConfig();
                pm.reloadFromConfig();
                msg(sender, "Configuration rechargée.");
                return true;
            }

            case "wand": {
                if (!adminOnly(sender)) return true;
                Player p = (Player) sender;
                p.getInventory().addItem(new ItemStack(Material.STICK, 1));
                msg(sender, "Baguette donnée (Stick). Clic gauche = pos1, clic droit = pos2.");
                return true;
            }

            case "define": {
                if (!adminOnly(sender)) return true;
                if (args.length < 2) { msg(sender, "Usage: /prot define <id> [base]"); return true; }

                Player p = (Player) sender;
                SelectionManager.Pair sel = sm.get(p.getUniqueId());
                if (sel == null || sel.a == null || sel.b == null) {
                    msg(sender, "Sélection incomplète. Utilise d’abord la baguette (pos1 & pos2).");
                    return true;
                }

                World w = sel.a.getWorld();
                if (w == null || sel.b.getWorld() == null || !w.getName().equals(sel.b.getWorld().getName())) {
                    msg(sender, "Les deux positions doivent être dans le même monde.");
                    return true;
                }

                String id   = args[1];
                String base = (args.length >= 3) ? args[2] : pm.resolveBaseForWorld(w.getName());

                ProtectedRegion r = new ProtectedRegion(id, base, sel.a, sel.b);
                pm.addRegion(r);
                msg(sender, "Région '" + id + "' ajoutée pour la base '" + base + "'. Elle s’appliquera aux clones.");
                return true;
            }

            case "list": {
                if (!sender.hasPermission("blitz2v2.protection.admin")) { msg(sender, "Permission manquante."); return true; }
                List<Map<?,?>> regs = plugin.getConfig().getMapList("protection.regions");
                if (regs.isEmpty()) { msg(sender, "Aucune région configurée."); return true; }
                sender.sendMessage(ChatColor.YELLOW + "[Prot] " + ChatColor.GOLD + regs.size() + ChatColor.WHITE + " région(s) :");
                for (Map<?,?> m : regs) {
                    String id   = String.valueOf(m.get("id"));
                    String base = String.valueOf(m.get("base"));
                    sender.sendMessage(ChatColor.DARK_GRAY + "- " + ChatColor.WHITE + id + ChatColor.GRAY + " @ " + base);
                }
                return true;
            }

            case "test": {
                if (!adminOnly(sender)) return true;
                Player p = (Player) sender;
                boolean prot = pm.isProtected(p.getLocation());
                msg(sender, "Ici : " + (prot ? "protégé" : "libre"));
                return true;
            }
        }

        msg(sender, "Sous-commandes : wand | define <id> [base] | list | reload | test");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("wand","define","list","reload","test")
                    .stream().filter(x -> x.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
