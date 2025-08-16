package fr.cronowz.blitz2v2.chat;

import fr.cronowz.blitz2v2.Blitz2v2;
import org.bukkit.World;

public class ChatPhaseResolver {

    public enum Phase { LOBBY, WAITING, GAME, OTHER }

    private final String lobbyBase;
    private final String waitingBase;
    private final String gameBase;

    public ChatPhaseResolver(Blitz2v2 plugin) {
        // On lit tes clés existantes; waiting n'était pas explicitement dans ta config → défaut sensé.
        this.lobbyBase   = plugin.getCfg().getString("lobby.world", "world_lobby");
        this.waitingBase = plugin.getCfg().getString("waiting.world", "world_waiting");
        this.gameBase    = plugin.getCfg().getString("game.world", "world_game");
    }

    public Phase resolve(World w) {
        if (w == null) return Phase.OTHER;
        String name = w.getName();
        if (inFamily(name, lobbyBase))   return Phase.LOBBY;
        if (inFamily(name, waitingBase)) return Phase.WAITING;
        if (inFamily(name, gameBase))    return Phase.GAME;
        return Phase.OTHER;
    }

    private boolean inFamily(String worldName, String base) {
        if (worldName.equalsIgnoreCase(base)) return true;
        String low = worldName.toLowerCase();
        String b   = base.toLowerCase();
        return low.startsWith(b + "_") || low.startsWith(b + "-");
    }
}
