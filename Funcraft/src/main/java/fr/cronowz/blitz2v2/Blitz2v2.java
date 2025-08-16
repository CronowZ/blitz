// src/main/java/fr/cronowz/blitz2v2/Blitz2v2.java
package fr.cronowz.blitz2v2;

import fr.cronowz.blitz2v2.commands.HubCommand;
import fr.cronowz.blitz2v2.commands.SpawnCommand;
import fr.cronowz.blitz2v2.commands.StatsCommand;
import fr.cronowz.blitz2v2.game.GameRulesListener;
import fr.cronowz.blitz2v2.game.KitService;
import fr.cronowz.blitz2v2.listeners.CommandRestrictionListener;
import fr.cronowz.blitz2v2.listeners.CombatListener;
import fr.cronowz.blitz2v2.listeners.CompassMenuListener;
import fr.cronowz.blitz2v2.listeners.CreatureSpawnListener;
import fr.cronowz.blitz2v2.listeners.GameExitListener;
import fr.cronowz.blitz2v2.listeners.InventoryProtectionListener;
import fr.cronowz.blitz2v2.listeners.JoinListener;
import fr.cronowz.blitz2v2.listeners.JoinQuitSuppressListener;
import fr.cronowz.blitz2v2.listeners.LobbyProtectionListener;
import fr.cronowz.blitz2v2.listeners.PartyListener;
import fr.cronowz.blitz2v2.listeners.SubMenuListener;
import fr.cronowz.blitz2v2.listeners.TeamFriendlyFireListener;
import fr.cronowz.blitz2v2.listeners.TeamSelectListener;
import fr.cronowz.blitz2v2.listeners.WaitingRoomListener;
import fr.cronowz.blitz2v2.listeners.WaitingRoomVoidListener;
import fr.cronowz.blitz2v2.listeners.WeatherLockListener;
import fr.cronowz.blitz2v2.manager.GameManager;
import fr.cronowz.blitz2v2.combat.CombatTrackerListener;
import fr.cronowz.blitz2v2.stats.KillStatsManager;
import fr.cronowz.blitz2v2.protect.ProtectionListener;
import fr.cronowz.blitz2v2.protect.ProtectionManager;
import fr.cronowz.blitz2v2.protect.SelectionManager;
import fr.cronowz.blitz2v2.protect.WandListener;
import fr.cronowz.blitz2v2.protect.cmd.ProtectionCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Blitz2v2 extends JavaPlugin {

    private static Blitz2v2 instance;

    private GameManager       gameManager;
    private KitService        kitService;
    private KillStatsManager  killStatsManager;
    private FileConfiguration cfg;
    private String            gameWorldName;
    private Location          lobbySpawn;

    // Protection
    private ProtectionManager protectionManager;
    private SelectionManager  selectionManager;

    public static Blitz2v2 getInstance() { return instance; }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.cfg = getConfig();
        this.gameWorldName = cfg.getString("game.world", "world_game");

        String lobbyWorld = cfg.getString("lobby.world", "world_lobby");
        double lx = cfg.getDouble("lobby.x", 545);
        double ly = cfg.getDouble("lobby.y", 50);
        double lz = cfg.getDouble("lobby.z", 415);
        World lw = Bukkit.getWorld(lobbyWorld);
        if (lw != null) {
            this.lobbySpawn = new Location(lw, lx, ly, lz);
        } else {
            getLogger().warning("Monde de lobby introuvable : " + lobbyWorld);
        }

        int potatoAmount = cfg.getInt("kit.potatoes", 12);
        int pickDur      = cfg.getInt("kit.pickaxe-remaining-durability", 20);
        this.kitService       = new KitService(potatoAmount, pickDur);
        this.gameManager      = new GameManager();
        this.killStatsManager = new KillStatsManager(getDataFolder());

        // Nettoyage en cas de /reload
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);

        // Désactive météo & mobs dans tous les mondes déjà chargés
        setupWorlds();

        // === Protection init ===
        this.protectionManager = new ProtectionManager(this);
        this.selectionManager  = new SelectionManager();
        protectionManager.reloadFromConfig(); // charge bases + régions + réglages coffres

        // === Listeners / commandes ===
        PluginManager pm = getServer().getPluginManager();

        // Core lobby & jeu (tes listeners existants)
        pm.registerEvents(new JoinQuitSuppressListener(), this);
        pm.registerEvents(new JoinListener(),              this);
        pm.registerEvents(new InventoryProtectionListener(), this);
        pm.registerEvents(new CompassMenuListener(),       this);
        pm.registerEvents(new SubMenuListener(),           this);
        pm.registerEvents(new CommandRestrictionListener(), this);
        pm.registerEvents(new CombatListener(),            this);
        pm.registerEvents(new CombatTrackerListener(killStatsManager), this);
        pm.registerEvents(new TeamFriendlyFireListener(),  this);
        pm.registerEvents(new WaitingRoomVoidListener(),   this);
        pm.registerEvents(new PartyListener(),             this);
        pm.registerEvents(new WaitingRoomListener(),       this);
        pm.registerEvents(new GameExitListener(),          this);
        pm.registerEvents(new LobbyProtectionListener(),   this);
        pm.registerEvents(new TeamSelectListener(),        this);
        pm.registerEvents(new CreatureSpawnListener(),     this);
        pm.registerEvents(new WeatherLockListener(),       this);

        // Règles de jeu (kit, drops, respawn…)
        pm.registerEvents(new GameRulesListener(this, kitService, gameWorldName), this);

        // === Protection (wand + protection de blocs) ===
        pm.registerEvents(new WandListener(selectionManager), this);
        pm.registerEvents(new ProtectionListener(protectionManager), this);

        // === Chats séparés (Lobby / Waiting / Game + @public en game) ===
        pm.registerEvents(new fr.cronowz.blitz2v2.chat.SeparatedChatListener(this), this);

        // Commandes
        if (getCommand("prot") != null) {
            ProtectionCommand pc = new ProtectionCommand(this, protectionManager, selectionManager);
            getCommand("prot").setExecutor(pc);
            getCommand("prot").setTabCompleter(pc);
        }
        if (getCommand("hub")   != null) getCommand("hub").setExecutor(new HubCommand());
        if (getCommand("spawn") != null) getCommand("spawn").setExecutor(new SpawnCommand());
        if (getCommand("stats") != null) getCommand("stats").setExecutor(new StatsCommand(killStatsManager));

        getLogger().info("Blitz2v2 activé !");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
        if (killStatsManager != null) killStatsManager.save();
        getLogger().info("Blitz2v2 désactivé.");
    }

    /** Désactive la météo et le spawn de mobs dans tous les mondes chargés. */
    private void setupWorlds() {
        for (World w : Bukkit.getWorlds()) {
            try {
                w.setStorm(false);
                w.setThundering(false);
                w.setWeatherDuration(0);
                w.setThunderDuration(0);
                w.setGameRuleValue("doWeatherCycle", "false");
                w.setGameRuleValue("doMobSpawning", "false");
            } catch (Throwable ignored) { }
        }
    }

    /* === GETTERS === */
    public FileConfiguration getCfg()           { return cfg; }
    public GameManager       getGameManager()   { return gameManager; }
    public KitService        getKitService()    { return kitService; }
    public String            getGameWorldName() { return gameWorldName; }
    /** Spawn du lobby, pour le retour des joueurs */
    public Location          getLobbySpawn()    { return lobbySpawn; }
}
