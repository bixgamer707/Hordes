package me.bixgamer707.hordes;

import me.bixgamer707.hordes.arena.ArenaManager;
import me.bixgamer707.hordes.bossbar.BossBarManager;
import me.bixgamer707.hordes.commands.HordesAdminCommand;
import me.bixgamer707.hordes.commands.HordesCommand;
import me.bixgamer707.hordes.cooldown.CooldownManager;
import me.bixgamer707.hordes.file.FileManager;
import me.bixgamer707.hordes.gui.GUIListener;
import me.bixgamer707.hordes.leaderboard.LeaderboardManager;
import me.bixgamer707.hordes.listeners.EntityListener;
import me.bixgamer707.hordes.listeners.PlayerListener;
import me.bixgamer707.hordes.listeners.WorldGuardListener;
import me.bixgamer707.hordes.mob.MythicMobsIntegration;
import me.bixgamer707.hordes.mob.VanillaMobHandler;
import me.bixgamer707.hordes.placeholder.HordesExpansion;
import me.bixgamer707.hordes.rewards.RewardManager;
import me.bixgamer707.hordes.sound.SoundManager;
import me.bixgamer707.hordes.statistics.StatisticsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Main plugin class for Hordes
 * Dungeon/Arena system with configurable waves and rewards
 * 
 * @author bixgamer707
 * @version 2.0.0
 */
public class Hordes extends JavaPlugin {

    private static Hordes instance;
    
    // Managers
    private FileManager fileManager;
    private ArenaManager arenaManager;
    private CooldownManager cooldownManager;
    private RewardManager rewardManager;
    private StatisticsManager statisticsManager;
    private LeaderboardManager leaderboardManager;
    private BossBarManager bossBarManager;
    private SoundManager soundManager;
    
    // Integrations
    private MythicMobsIntegration mythicMobsIntegration;
    private VanillaMobHandler vanillaMobHandler;
    
    // Listeners
    private EntityListener entityListener;
    private PlayerListener playerListener;
    private WorldGuardListener worldGuardListener;
    private GUIListener guiListener;
    
    // Tasks
    private BukkitRunnable cooldownCleanupTask;
    private BukkitRunnable itemTrackingCleanupTask;

    @Override
    public void onEnable() {
        instance = this;
        
        long startTime = System.currentTimeMillis();
        
        // ASCII art
        logInfo("========================================");
        logInfo("  _   _                _");
        logInfo(" | | | | ___  _ __ __| | ___  ___");
        logInfo(" | |_| |/ _ \\| '__/ _` |/ _ \\/ __|");
        logInfo(" |  _  | (_) | | | (_| |  __/\\__ \\");
        logInfo(" |_| |_|\\___/|_|  \\__,_|\\___||___/");
        logInfo("");
        logInfo(" Version: " + getDescription().getVersion());
        logInfo(" Author: bixgamer707");
        logInfo("========================================");
        
        // Initialize managers
        if (!initializeManagers()) {
            logError("Failed to initialize managers - disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Load configurations
        loadConfigurations();
        
        // Register listeners
        registerListeners();
        
        // Register commands
        registerCommands();
        
        // Register PlaceholderAPI
        registerPlaceholders();
        
        // Start cleanup tasks
        startCleanupTasks();
        
        long loadTime = System.currentTimeMillis() - startTime;
        logInfo("Plugin enabled successfully in " + loadTime + "ms!");
        logInfo("========================================");
    }

    @Override
    public void onDisable() {
        logInfo("Shutting down Hordes...");
        
        // Stop cleanup tasks
        if (cooldownCleanupTask != null) {
            cooldownCleanupTask.cancel();
        }
        
        if (itemTrackingCleanupTask != null) {
            itemTrackingCleanupTask.cancel();
        }
        
        // Shutdown arenas
        if (arenaManager != null) {
            arenaManager.shutdown();
        }
        
        // Cleanup boss bars
        if (bossBarManager != null) {
            bossBarManager.cleanup();
        }
        
        // Save statistics
        if (statisticsManager != null) {
            statisticsManager.save();
        }
        
        // Save configurations
        if (fileManager != null) {
            fileManager.saveFiles();
        }
        
        logInfo("Plugin disabled successfully!");
    }

    /**
     * Initializes all managers
     * 
     * @return true if successful
     */
    private boolean initializeManagers() {
        try {
            logInfo("Initializing managers...");
            
            // File manager (must be first)
            fileManager = new FileManager(this);
            fileManager.loadFiles();
            
            // Core managers
            cooldownManager = new CooldownManager();
            rewardManager = new RewardManager(this);
            arenaManager = new ArenaManager(this);
            statisticsManager = new StatisticsManager(this);
            leaderboardManager = new LeaderboardManager(this);
            bossBarManager = new BossBarManager(this);
            soundManager = new SoundManager(this);
            
            // Mob handlers
            mythicMobsIntegration = new MythicMobsIntegration(this);
            vanillaMobHandler = new VanillaMobHandler();
            
            logInfo("All managers initialized successfully");
            return true;
            
        } catch (Exception e) {
            logError("Failed to initialize managers: " + e.getMessage());
            if (fileManager != null && fileManager.getFile("config.yml").getBoolean("debug-mode", false)) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Loads all configurations
     */
    private void loadConfigurations() {
        logInfo("Loading configurations...");
        
        // Load arenas
        arenaManager.loadArenas();
        
        logInfo("Configurations loaded");
    }

    /**
     * Registers all event listeners
     */
    private void registerListeners() {
        logInfo("Registering listeners...");
        
        PluginManager pm = getServer().getPluginManager();
        
        // Entity listener
        entityListener = new EntityListener(this);
        pm.registerEvents(entityListener, this);
        
        // Player listener
        playerListener = new PlayerListener(this);
        pm.registerEvents(playerListener, this);
        
        // WorldGuard listener (if available)
        worldGuardListener = new WorldGuardListener(this);
        if (worldGuardListener.isWorldGuardEnabled()) {
            pm.registerEvents(worldGuardListener, this);
        }
        
        // GUI listener
        guiListener = new GUIListener();
        pm.registerEvents(guiListener, this);
        
        logInfo("Listeners registered");
    }

    /**
     * Registers all commands
     */
    private void registerCommands() {
        logInfo("Registering commands...");
        
        // Player command
        HordesCommand hordesCommand = new HordesCommand(this);
        getCommand("hordes").setExecutor(hordesCommand);
        getCommand("hordes").setTabCompleter(hordesCommand);
        
        // Admin command
        HordesAdminCommand adminCommand = new HordesAdminCommand(this);
        getCommand("hordesadmin").setExecutor(adminCommand);
        getCommand("hordesadmin").setTabCompleter(adminCommand);
        
        logInfo("Commands registered");
    }

    /**
     * Starts periodic cleanup tasks
     */
    private void startCleanupTasks() {
        // Cooldown cleanup task (runs every 5 minutes)
        cooldownCleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cooldownManager.cleanupExpired();
            }
        };
        
        cooldownCleanupTask.runTaskTimerAsynchronously(this, 6000L, 6000L); // 5 minutes
        
        // Item tracking cleanup task (runs every minute)
        itemTrackingCleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (playerListener != null) {
                    playerListener.cleanupItemTracking();
                }
            }
        };
        
        itemTrackingCleanupTask.runTaskTimer(this, 1200L, 1200L); // 1 minute
        
        logInfo("Cleanup tasks started");
    }

    /**
     * Registers PlaceholderAPI expansion
     */
    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new HordesExpansion(this).register();
                logInfo("PlaceholderAPI expansion registered");
            } catch (Exception e) {
                logWarning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
            }
        } else {
            logInfo("PlaceholderAPI not found - placeholders disabled");
        }
    }

    /**
     * Reloads the plugin
     */
    public void reload() {
        logInfo("Reloading plugin...");
        
        // Reload files
        fileManager.reload();
        
        // Reload arenas
        arenaManager.reloadArenas();
        
        // Reload integrations
        mythicMobsIntegration.reload();
        rewardManager.reload();
        
        logInfo("Plugin reloaded successfully");
    }

    // Getters
    
    public static Hordes getInstance() {
        return instance;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public MythicMobsIntegration getMythicMobsIntegration() {
        return mythicMobsIntegration;
    }

    public VanillaMobHandler getVanillaMobHandler() {
        return vanillaMobHandler;
    }

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    // Utility methods
    
    public void logInfo(String message) {
        Bukkit.getLogger().info("[Hordes] " + message);
    }

    public void logWarning(String message) {
        Bukkit.getLogger().warning("[Hordes] " + message);
    }

    public void logError(String message) {
        Bukkit.getLogger().severe("[Hordes] " + message);
    }
}
