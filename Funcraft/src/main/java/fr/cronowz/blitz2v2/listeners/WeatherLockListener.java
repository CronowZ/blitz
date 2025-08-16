package fr.cronowz.blitz2v2.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class WeatherLockListener implements Listener {

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        apply(e.getWorld());
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent e) {
        if (e.toWeatherState()) { // it wants to start raining
            e.setCancelled(true);
            apply(e.getWorld());
        }
    }

    @EventHandler
    public void onThunderChange(ThunderChangeEvent e) {
        if (e.toThunderState()) { // it wants to start thundering
            e.setCancelled(true);
            apply(e.getWorld());
        }
    }

    private void apply(World w) {
        if (w == null) return;
        try {
            w.setStorm(false);
            w.setThundering(false);
            w.setWeatherDuration(0);
            w.setThunderDuration(0);
            w.setGameRuleValue("doWeatherCycle", "false");
        } catch (Throwable ignored) { }
    }
}
