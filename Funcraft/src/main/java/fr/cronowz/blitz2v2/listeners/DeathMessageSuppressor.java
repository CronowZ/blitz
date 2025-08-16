package fr.cronowz.blitz2v2.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/** Supprime les messages de mort par défaut de Bukkit. */
public class DeathMessageSuppressor implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.setDeathMessage(null);
    }
}

