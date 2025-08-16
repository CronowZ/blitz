// src/main/java/fr/cronowz/blitz2v2/protect/SelectionManager.java
package fr.cronowz.blitz2v2.protect;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {

    public static class Pair {
        public Location a;
        public Location b;
    }

    private final Map<UUID, Pair> selections = new HashMap<>();

    public void setPos1(UUID uid, Location l) {
        Pair p = selections.computeIfAbsent(uid, k -> new Pair());
        p.a = l;
    }

    public void setPos2(UUID uid, Location l) {
        Pair p = selections.computeIfAbsent(uid, k -> new Pair());
        p.b = l;
    }

    public Pair get(UUID uid) {
        return selections.get(uid);
    }

    public void clear(UUID uid) {
        selections.remove(uid);
    }
}
