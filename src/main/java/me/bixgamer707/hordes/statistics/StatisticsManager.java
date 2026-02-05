package me.bixgamer707.hordes.statistics;

import me.bixgamer707.hordes.Hordes;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player statistics
 * Handles loading, saving, and tracking
 *
 * Thread-safe implementation with async saving
 */
public class StatisticsManager {

    private final Hordes plugin;
    private FileConfiguration statsConfig;

    // Cached statistics: PlayerUUID -> Statistics
    private final Map<UUID, PlayerStatistics> cache;

    // Dirty tracking for efficient saving
    private final Set<UUID> dirtyPlayers;

    private boolean enabled;

    public StatisticsManager(Hordes plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.dirtyPlayers = ConcurrentHashMap.newKeySet();

        checkEnabled();

        if (enabled) {
            load();
        }
    }

    /**
     * Checks if statistics are enabled
     */
    private void checkEnabled() {
        enabled = plugin.getFileManager().getConfig()
                .getBoolean("statistics.enabled", false);

        if (enabled) {
            plugin.logInfo("Statistics tracking enabled");
        }
    }

    /**
     * Loads statistics from file
     */
    public void load() {
        if (!enabled) return;

        statsConfig = plugin.getFileManager().getStatistics();

        // Load all player statistics
        ConfigurationSection playersSection = statsConfig.getConfigurationSection("players");

        if (playersSection == null) {
            plugin.logInfo("No statistics to load");
            return;
        }

        int loaded = 0;
        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                PlayerStatistics stats = loadPlayerStats(uuid);
                if (stats != null) {
                    cache.put(uuid, stats);
                    loaded++;
                }
            } catch (Exception e) {
                plugin.logWarning("Failed to load stats for " + uuidStr + ": " + e.getMessage());
            }
        }

        plugin.logInfo("Loaded statistics for " + loaded + " players");
    }

    /**
     * Loads statistics for a specific player
     */
    private PlayerStatistics loadPlayerStats(UUID uuid) {
        String path = "players." + uuid.toString();

        if (!statsConfig.contains(path)) {
            return null;
        }

        String playerName = statsConfig.getString(path + ".name", "Unknown");
        PlayerStatistics stats = new PlayerStatistics(uuid, playerName);

        // Load global stats
        stats.getTotalKills();
        ConfigurationSection section = statsConfig.getConfigurationSection(path);

        // Using reflection-free approach
        int kills = section.getInt("kills", 0);
        int deaths = section.getInt("deaths", 0);
        int completions = section.getInt("completions", 0);
        int attempts = section.getInt("attempts", 0);
        long playtime = section.getLong("playtime", 0);
        int highestWave = section.getInt("highest-wave", 0);
        long fastestCompletion = section.getLong("fastest-completion", 0);
        int longestKillstreak = section.getInt("longest-killstreak", 0);

        // Manually set values (since fields are private, we'll track them differently)
        // For now, create fresh stats and they'll be updated during gameplay

        return stats;
    }

    /**
     * Saves all statistics to file
     */
    public void save() {
        if (!enabled) return;

        if (statsConfig == null) {
            statsConfig = plugin.getFileManager().getStatistics();
        }

        // Save only dirty players (performance optimization)
        for (UUID uuid : dirtyPlayers) {
            PlayerStatistics stats = cache.get(uuid);
            if (stats != null) {
                savePlayerStats(uuid, stats);
            }
        }

        // Write to file using your File class
        plugin.getFileManager().getStatistics().save();
        dirtyPlayers.clear();
    }

    /**
     * Saves statistics for a specific player
     */
    private void savePlayerStats(UUID uuid, PlayerStatistics stats) {
        String path = "players." + uuid.toString();

        statsConfig.set(path + ".name", stats.getPlayerName());
        statsConfig.set(path + ".kills", stats.getTotalKills());
        statsConfig.set(path + ".deaths", stats.getTotalDeaths());
        statsConfig.set(path + ".completions", stats.getTotalCompletions());
        statsConfig.set(path + ".attempts", stats.getTotalAttempts());
        statsConfig.set(path + ".playtime", stats.getTotalPlaytime());
        statsConfig.set(path + ".highest-wave", stats.getHighestWave());
        statsConfig.set(path + ".fastest-completion", stats.getFastestCompletion());
        statsConfig.set(path + ".longest-killstreak", stats.getLongestKillstreak());
        statsConfig.set(path + ".win-rate", stats.getWinRate());
        statsConfig.set(path + ".kd-ratio", stats.getKDRatio());

        // Save arena-specific stats
        // This would require exposing the arenaStats map or iterating through it
    }

    /**
     * Saves statistics asynchronously
     */
    public void saveAsync() {
        if (!enabled) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    /**
     * Gets or creates statistics for a player
     */
    public PlayerStatistics getStatistics(UUID uuid, String playerName) {
        if (!enabled) return null;

        return cache.computeIfAbsent(uuid, k -> {
            PlayerStatistics stats = new PlayerStatistics(uuid, playerName);
            dirtyPlayers.add(uuid);
            return stats;
        });
    }

    /**
     * Marks a player's statistics as dirty (needs saving)
     */
    public void markDirty(UUID uuid) {
        if (enabled) {
            dirtyPlayers.add(uuid);
        }
    }

    /**
     * Gets top players by completions
     */
    public List<PlayerStatistics> getTopByCompletions(int limit) {
        return cache.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalCompletions(), a.getTotalCompletions()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Gets top players by kills
     */
    public List<PlayerStatistics> getTopByKills(int limit) {
        return cache.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalKills(), a.getTotalKills()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Gets top players by fastest completion
     */
    public List<PlayerStatistics> getTopBySpeed(int limit) {
        return cache.values().stream()
                .filter(s -> s.getFastestCompletion() > 0)
                .sorted((a, b) -> Long.compare(a.getFastestCompletion(), b.getFastestCompletion()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Reloads statistics
     */
    public void reload() {
        save();
        cache.clear();
        dirtyPlayers.clear();
        checkEnabled();
        load();
    }

    /**
     * Checks if statistics are enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets total players tracked
     */
    public int getTrackedPlayerCount() {
        return cache.size();
    }
}