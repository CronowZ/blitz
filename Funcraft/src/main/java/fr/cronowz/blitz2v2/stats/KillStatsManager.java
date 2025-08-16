package fr.cronowz.blitz2v2.stats;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillStatsManager {

    public static class Stats {
        public int kills;
        public int deaths;
        public int assists;
    }

    private final Map<UUID, Stats> data = new HashMap<>();
    private final File file;

    public KillStatsManager(File dataFolder) {
        this.file = new File(dataFolder, "killstats.yml");
        load();
    }

    public Stats get(UUID id) {
        return data.computeIfAbsent(id, k -> new Stats());
    }

    public void addKill(UUID id)    { get(id).kills++; }
    public void addDeath(UUID id)   { get(id).deaths++; }
    public void addAssist(UUID id)  { get(id).assists++; }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Stats> e : data.entrySet()) {
            String k = e.getKey().toString();
            y.set(k + ".kills",   e.getValue().kills);
            y.set(k + ".deaths",  e.getValue().deaths);
            y.set(k + ".assists", e.getValue().assists);
        }
        try { y.save(file); } catch (IOException ignored) {}
    }

    public void load() {
        data.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String k : y.getKeys(false)) {
            try {
                UUID id = UUID.fromString(k);
                Stats s = new Stats();
                s.kills   = y.getInt(k + ".kills", 0);
                s.deaths  = y.getInt(k + ".deaths", 0);
                s.assists = y.getInt(k + ".assists", 0);
                data.put(id, s);
            } catch (Exception ignored) {}
        }
    }
}
