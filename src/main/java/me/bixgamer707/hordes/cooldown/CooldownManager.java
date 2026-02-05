package me.bixgamer707.hordes.cooldown;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player cooldowns for arenas
 * Thread-safe implementation with concurrent collections
 * 
 * Supports:
 * - Per-arena cooldowns
 * - Global cooldowns (all arenas)
 * - Temporary cooldowns (for rejoin mechanics)
 */
public class CooldownManager {

    // Arena-specific cooldowns: UUID -> (ArenaID -> ExpiryTime)
    private final Map<UUID, Map<String, Long>> arenaCooldowns;
    
    // Global cooldowns: UUID -> ExpiryTime
    private final Map<UUID, Long> globalCooldowns;
    
    // Temporary cooldowns (for death rejoin): UUID -> (ArenaID -> ExpiryTime)
    private final Map<UUID, Map<String, Long>> tempCooldowns;

    public CooldownManager() {
        this.arenaCooldowns = new ConcurrentHashMap<>();
        this.globalCooldowns = new ConcurrentHashMap<>();
        this.tempCooldowns = new ConcurrentHashMap<>();
    }

    /**
     * Sets a cooldown for a specific arena
     * 
     * @param player Player UUID
     * @param arenaId Arena ID
     * @param duration Duration in seconds
     */
    public void setCooldown(UUID player, String arenaId, long duration) {
        long expiryTime = System.currentTimeMillis() + (duration * 1000);
        
        arenaCooldowns.computeIfAbsent(player, k -> new ConcurrentHashMap<>())
            .put(arenaId, expiryTime);
    }

    /**
     * Sets a global cooldown (applies to all arenas)
     * 
     * @param player Player UUID
     * @param duration Duration in seconds
     */
    public void setGlobalCooldown(UUID player, long duration) {
        long expiryTime = System.currentTimeMillis() + (duration * 1000);
        globalCooldowns.put(player, expiryTime);
    }

    /**
     * Sets a temporary cooldown (for rejoin mechanics)
     * 
     * @param player Player UUID
     * @param arenaId Arena ID
     * @param duration Duration in seconds
     */
    public void setTempCooldown(UUID player, String arenaId, long duration) {
        long expiryTime = System.currentTimeMillis() + (duration * 1000);
        
        tempCooldowns.computeIfAbsent(player, k -> new ConcurrentHashMap<>())
            .put(arenaId, expiryTime);
    }

    /**
     * Checks if player has any active cooldown for an arena
     * 
     * @param player Player UUID
     * @param arenaId Arena ID
     * @return true if cooldown is active
     */
    public boolean hasCooldown(UUID player, String arenaId) {
        // Check global cooldown first
        if (hasGlobalCooldown(player)) {
            return true;
        }
        
        // Check arena-specific cooldown
        if (hasArenaCooldown(player, arenaId)) {
            return true;
        }
        
        // Check temporary cooldown
        return hasTempCooldown(player, arenaId);
    }

    /**
     * Checks if player has a global cooldown
     */
    public boolean hasGlobalCooldown(UUID player) {
        Long expiry = globalCooldowns.get(player);
        
        if (expiry == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= expiry) {
            globalCooldowns.remove(player);
            return false;
        }
        
        return true;
    }

    /**
     * Checks if player has an arena-specific cooldown
     */
    public boolean hasArenaCooldown(UUID player, String arenaId) {
        Map<String, Long> playerCooldowns = arenaCooldowns.get(player);
        
        if (playerCooldowns == null) {
            return false;
        }
        
        Long expiry = playerCooldowns.get(arenaId);
        
        if (expiry == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= expiry) {
            playerCooldowns.remove(arenaId);
            return false;
        }
        
        return true;
    }

    /**
     * Checks if player has a temporary cooldown
     */
    public boolean hasTempCooldown(UUID player, String arenaId) {
        Map<String, Long> playerTempCooldowns = tempCooldowns.get(player);
        
        if (playerTempCooldowns == null) {
            return false;
        }
        
        Long expiry = playerTempCooldowns.get(arenaId);
        
        if (expiry == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= expiry) {
            playerTempCooldowns.remove(arenaId);
            return false;
        }
        
        return true;
    }

    /**
     * Gets remaining cooldown time in a human-readable format
     * 
     * @param player Player UUID
     * @param arenaId Arena ID
     * @return Formatted time string (e.g., "5m 30s")
     */
    public String getRemainingTime(UUID player, String arenaId) {
        long remaining = getRemainingMillis(player, arenaId);
        
        if (remaining <= 0) {
            return "0s";
        }
        
        return formatDuration(remaining);
    }

    /**
     * Gets remaining cooldown in milliseconds
     */
    public long getRemainingMillis(UUID player, String arenaId) {
        long currentTime = System.currentTimeMillis();
        long maxExpiry = 0;
        
        // Check global cooldown
        Long globalExpiry = globalCooldowns.get(player);
        if (globalExpiry != null && globalExpiry > currentTime) {
            maxExpiry = Math.max(maxExpiry, globalExpiry);
        }
        
        // Check arena cooldown
        Map<String, Long> playerCooldowns = arenaCooldowns.get(player);
        if (playerCooldowns != null) {
            Long arenaExpiry = playerCooldowns.get(arenaId);
            if (arenaExpiry != null && arenaExpiry > currentTime) {
                maxExpiry = Math.max(maxExpiry, arenaExpiry);
            }
        }
        
        // Check temp cooldown
        Map<String, Long> playerTempCooldowns = tempCooldowns.get(player);
        if (playerTempCooldowns != null) {
            Long tempExpiry = playerTempCooldowns.get(arenaId);
            if (tempExpiry != null && tempExpiry > currentTime) {
                maxExpiry = Math.max(maxExpiry, tempExpiry);
            }
        }
        
        return maxExpiry - currentTime;
    }

    /**
     * Formats duration in milliseconds to readable string
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        
        if (seconds < 60) {
            return seconds + "s";
        }
        
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes < 60) {
            return minutes + "m " + seconds + "s";
        }
        
        long hours = minutes / 60;
        minutes = minutes % 60;
        
        if (hours < 24) {
            return hours + "h " + minutes + "m";
        }
        
        long days = hours / 24;
        hours = hours % 24;
        
        return days + "d " + hours + "h";
    }

    /**
     * Removes all cooldowns for a player
     */
    public void clearCooldowns(UUID player) {
        arenaCooldowns.remove(player);
        globalCooldowns.remove(player);
        tempCooldowns.remove(player);
    }

    /**
     * Removes cooldown for specific arena
     */
    public void clearArenaCooldown(UUID player, String arenaId) {
        Map<String, Long> playerCooldowns = arenaCooldowns.get(player);
        if (playerCooldowns != null) {
            playerCooldowns.remove(arenaId);
        }
        
        Map<String, Long> playerTempCooldowns = tempCooldowns.get(player);
        if (playerTempCooldowns != null) {
            playerTempCooldowns.remove(arenaId);
        }
    }

    /**
     * Clears expired cooldowns (cleanup task)
     * Should be called periodically to prevent memory leaks
     */
    public void cleanupExpired() {
        long currentTime = System.currentTimeMillis();
        
        // Clean global cooldowns
        globalCooldowns.entrySet().removeIf(entry -> 
            entry.getValue() <= currentTime
        );
        
        // Clean arena cooldowns
        arenaCooldowns.values().forEach(map -> 
            map.entrySet().removeIf(entry -> entry.getValue() <= currentTime)
        );
        arenaCooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // Clean temp cooldowns
        tempCooldowns.values().forEach(map -> 
            map.entrySet().removeIf(entry -> entry.getValue() <= currentTime)
        );
        tempCooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Gets total number of active cooldowns (for debugging)
     */
    public int getActiveCooldownCount() {
        int count = globalCooldowns.size();
        count += arenaCooldowns.values().stream()
            .mapToInt(Map::size)
            .sum();
        count += tempCooldowns.values().stream()
            .mapToInt(Map::size)
            .sum();
        return count;
    }
}
