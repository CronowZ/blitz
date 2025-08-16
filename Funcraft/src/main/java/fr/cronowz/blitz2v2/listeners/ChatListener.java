package fr.cronowz.blitz2v2.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        e.setCancelled(true);
        Player p = e.getPlayer();
        String nick = p.getDisplayName(); 
        String formatted = "§7★ §f" + nick + " §7» §f" + e.getMessage();
        Bukkit.getOnlinePlayers().forEach(pl -> pl.sendMessage(formatted));
    }
}
