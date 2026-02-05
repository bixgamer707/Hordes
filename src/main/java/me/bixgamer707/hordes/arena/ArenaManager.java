package me.bixgamer707.hordes.arena;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.config.ArenaConfig;
import me.bixgamer707.hordes.file.File;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all arena instances
 * Handles loading, unloading, and arena lifecycle

 * Thread-safe singleton implementation
 */
public class ArenaManager {

    private final Hordes plugin;
    
    // All registered arenas: ArenaID -> Arena
    private final Map<String, Arena> arenas;
    
    // Player to arena mapping for fast lookup: PlayerUUID -> ArenaID
    private final Map<UUID, String> playerArenas;
    
    // WorldGuard region to arena mapping: RegionName -> ArenaID
    private final Map<String, String> regionArenas;

    public ArenaManager(Hordes plugin) {
        this.plugin = plugin;
        this.arenas = new ConcurrentHashMap<>();
        this.playerArenas = new ConcurrentHashMap<>();
        this.regionArenas = new ConcurrentHashMap<>();
    }

    /**
     * Loads all arenas from configuration
     */
    public void loadArenas() {
        File configFile = plugin.getFileManager().getArenas();
        ConfigurationSection arenasSection = configFile.getConfigurationSection("arenas");
        
        if (arenasSection == null) {
            Bukkit.getLogger().warning("[Hordes] No arenas configured in arenas.yml");
            return;
        }
        
        int loaded = 0;
        int failed = 0;
        
        for (String arenaId : arenasSection.getKeys(false)) {
            try {
                ConfigurationSection arenaSection = arenasSection.getConfigurationSection(arenaId);
                ArenaConfig config = ArenaConfig.load(arenaId, arenaSection);
                
                if (!config.isValid()) {
                    Bukkit.getLogger().warning("[Hordes] Arena " + arenaId + " has invalid configuration");
                    failed++;
                    continue;
                }
                
                Arena arena = new Arena(arenaId, config, plugin);
                arenas.put(arenaId, arena);
                
                // Register WorldGuard region if configured
                if (config.hasWorldGuardRegion()) {
                    regionArenas.put(config.getWorldGuardRegion(), arenaId);
                }
                
                loaded++;
                
            } catch (Exception e) {
                plugin.logError("Failed to load arena " + arenaId + ": " + e.getMessage());
                if (plugin.getFileManager().getFile("config.yml").getBoolean("debug-mode", false)) {
                    e.printStackTrace();
                }
                failed++;
            }
        }
        
        Bukkit.getLogger().info("[Hordes] Loaded " + loaded + " arenas (" + failed + " failed)");
    }

    /**
     * Reloads all arenas
     * WARNING: This will end all active arenas
     */
    public void reloadArenas() {
        Bukkit.getLogger().info("[Hordes] Reloading arenas...");
        
        // End all active arenas
        for (Arena arena : arenas.values()) {
            if (arena.getState().isActive()) {
                arena.endArena(false);
            }
        }
        
        // Clear all mappings
        arenas.clear();
        playerArenas.clear();
        regionArenas.clear();
        
        // Reload from config
        loadArenas();
    }

    /**
     * Gets an arena by ID
     * 
     * @param arenaId Arena identifier
     * @return Arena or null if not found
     */
    public Arena getArena(String arenaId) {
        return arenas.get(arenaId);
    }

    /**
     * Gets arena by WorldGuard region
     * 
     * @param regionName Region name
     * @return Arena or null if not found
     */
    public Arena getArenaByRegion(String regionName) {
        String arenaId = regionArenas.get(regionName);
        return arenaId != null ? arenas.get(arenaId) : null;
    }

    /**
     * Gets the arena a player is currently in
     * 
     * @param player Player UUID
     * @return Arena or null if not in any arena
     */
    public Arena getPlayerArena(UUID player) {
        String arenaId = playerArenas.get(player);
        return arenaId != null ? arenas.get(arenaId) : null;
    }

    /**
     * Gets the arena a player is currently in
     * 
     * @param player Player
     * @return Arena or null if not in any arena
     */
    public Arena getPlayerArena(Player player) {
        return getPlayerArena(player.getUniqueId());
    }

    /**
     * Registers a player to an arena
     * Called by Arena when player joins
     * 
     * @param player Player UUID
     * @param arenaId Arena ID
     */
    public void registerPlayer(UUID player, String arenaId) {
        playerArenas.put(player, arenaId);
    }

    /**
     * Unregisters a player from their arena
     * Called by Arena when player leaves
     * 
     * @param player Player UUID
     */
    public void unregisterPlayer(UUID player) {
        playerArenas.remove(player);
    }

    /**
     * Checks if player is in any arena
     * 
     * @param player Player UUID
     * @return true if player is in an arena
     */
    public boolean isInArena(UUID player) {
        return playerArenas.containsKey(player);
    }

    /**
     * Checks if player is in any arena
     * 
     * @param player Player
     * @return true if player is in an arena
     */
    public boolean isInArena(Player player) {
        return isInArena(player.getUniqueId());
    }

    /**
     * Gets all registered arenas
     * 
     * @return Unmodifiable map of arenas
     */
    public Map<String, Arena> getArenas() {
        return Collections.unmodifiableMap(arenas);
    }

    /**
     * Gets all enabled arenas
     * 
     * @return List of enabled arenas
     */
    public List<Arena> getEnabledArenas() {
        List<Arena> enabled = new ArrayList<>();
        
        for (Arena arena : arenas.values()) {
            if (arena.getConfig().isEnabled()) {
                enabled.add(arena);
            }
        }
        
        return enabled;
    }

    /**
     * Gets arenas by state
     * 
     * @param state Arena state to filter
     * @return List of arenas in that state
     */
    public List<Arena> getArenasByState(ArenaState state) {
        List<Arena> result = new ArrayList<>();
        
        for (Arena arena : arenas.values()) {
            if (arena.getState() == state) {
                result.add(arena);
            }
        }
        
        return result;
    }

    /**
     * Gets all arena IDs
     * 
     * @return Set of arena IDs
     */
    public Set<String> getArenaIds() {
        return new HashSet<>(arenas.keySet());
    }

    /**
     * Gets count of total arenas
     * 
     * @return Number of arenas
     */
    public int getArenaCount() {
        return arenas.size();
    }

    /**
     * Gets count of active arenas
     * 
     * @return Number of active arenas
     */
    public int getActiveArenaCount() {
        return (int) arenas.values().stream()
            .filter(arena -> arena.getState().isActive())
            .count();
    }

    /**
     * Gets total player count across all arenas
     * 
     * @return Total players in arenas
     */
    public int getTotalPlayerCount() {
        return arenas.values().stream()
            .mapToInt(Arena::getPlayerCount)
            .sum();
    }

    /**
     * Attempts to join player to arena
     * 
     * @param player Player to join
     * @param arenaId Arena ID
     * @return true if successfully joined
     */
    public boolean joinArena(Player player, String arenaId) {
        // Check if already in an arena
        if (isInArena(player)) {
            return false;
        }
        
        Arena arena = getArena(arenaId);
        
        if (arena == null) {
            return false;
        }
        
        // Attempt to join
        boolean success = arena.joinPlayer(player);
        
        if (success) {
            registerPlayer(player.getUniqueId(), arenaId);
        }
        
        return success;
    }

    /**
     * Removes player from their current arena
     * 
     * @param player Player to remove
     * @param restore Whether to restore original state
     */
    public void leaveArena(Player player, boolean restore) {
        Arena arena = getPlayerArena(player);
        
        if (arena == null) {
            return;
        }
        
        arena.removePlayer(player, restore);
        unregisterPlayer(player.getUniqueId());
    }

    /**
     * Shuts down all arenas (called on plugin disable)
     */
    public void shutdown() {
        Bukkit.getLogger().info("[Hordes] Shutting down all arenas...");
        
        for (Arena arena : arenas.values()) {
            if (arena.getState() != ArenaState.WAITING) {
                arena.endArena(false);
            }
        }
        
        arenas.clear();
        playerArenas.clear();
        regionArenas.clear();
    }

    /**
     * Creates debug information about all arenas
     * 
     * @return Debug string
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Arena Manager Debug ===\n");
        sb.append("Total Arenas: ").append(getArenaCount()).append("\n");
        sb.append("Active Arenas: ").append(getActiveArenaCount()).append("\n");
        sb.append("Total Players: ").append(getTotalPlayerCount()).append("\n");
        sb.append("\nArena Details:\n");
        
        for (Arena arena : arenas.values()) {
            sb.append("  ").append(arena.getId()).append(": ");
            sb.append(arena.getState().getDisplayName()).append(" | ");
            sb.append("Players: ").append(arena.getPlayerCount()).append(" | ");
            sb.append("Wave: ").append(arena.getCurrentWaveNumber()).append("\n");
        }
        
        return sb.toString();
    }
}
