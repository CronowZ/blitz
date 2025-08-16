package fr.cronowz.blitz2v2.commands;

import fr.cronowz.blitz2v2.stats.KillStatsManager;
import fr.cronowz.blitz2v2.stats.KillStatsManager.Stats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class StatsCommand implements CommandExecutor {

    private final KillStatsManager stats;

    public StatsCommand(KillStatsManager stats) {
        this.stats = stats;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        OfflinePlayer target;

        if (args.length >= 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Joueur introuvable.");
                return true;
            }
        } else {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /stats <joueur>");
                return true;
            }
            target = (OfflinePlayer) sender;
        }

        UUID id = target.getUniqueId();
        Stats s = stats.get(id);
        double kd = s.deaths == 0 ? s.kills : (double) s.kills / (double) s.deaths;

        sender.sendMessage(ChatColor.GOLD + "Statistiques de " + ChatColor.WHITE + target.getName());
        sender.sendMessage(ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + s.kills
                + ChatColor.YELLOW + " | Assists: " + ChatColor.WHITE + s.assists
                + ChatColor.YELLOW + " | Morts: " + ChatColor.WHITE + s.deaths);
        sender.sendMessage(ChatColor.YELLOW + "K/D: " + ChatColor.WHITE + String.format(java.util.Locale.US, "%.2f", kd));
        return true;
    }
}
