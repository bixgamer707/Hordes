package me.bixgamer707.hordes.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores statistics for a single player
 * Tracks kills, deaths, completions, playtime, etc.
 */
public class PlayerStatistics {

    private final UUID playerUuid;
    private String playerName;
    
    // Global statistics
    private int totalKills;
    private int totalDeaths;
    private int totalCompletions;
    private int totalAttempts;
    private long totalPlaytime; // in seconds
    
    // Best records
    private int highestWave;
    private long fastestCompletion; // in seconds (0 = no completion)
    private int longestKillstreak;
    
    // Per-arena statistics: ArenaID -> Statistics
    private Map<String, ArenaStats> arenaStats;
    
    // Session data (temporary)
    private transient long sessionStart;
    private transient int sessionKills;

    public PlayerStatistics(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.arenaStats = new HashMap<>();
        this.fastestCompletion = 0;
        this.sessionStart = 0;
    }

    /**
     * Records a kill
     */
    public void addKill() {
        totalKills++;
        sessionKills++;
    }

    /**
     * Records a death
     */
    public void addDeath() {
        totalDeaths++;
        sessionKills = 0; // Reset killstreak
    }

    /**
     * Records an arena attempt
     */
    public void addAttempt(String arenaId) {
        totalAttempts++;
        getArenaStats(arenaId).attempts++;
    }

    /**
     * Records an arena completion
     */
    public void addCompletion(String arenaId, int wavesCompleted, long duration) {
        totalCompletions++;
        
        // Update highest wave
        if (wavesCompleted > highestWave) {
            highestWave = wavesCompleted;
        }
        
        // Update fastest completion
        if (fastestCompletion == 0 || duration < fastestCompletion) {
            fastestCompletion = duration;
        }
        
        // Update arena stats
        ArenaStats stats = getArenaStats(arenaId);
        stats.completions++;
        if (stats.fastestCompletion == 0 || duration < stats.fastestCompletion) {
            stats.fastestCompletion = duration;
        }
    }

    /**
     * Records playtime
     */
    public void addPlaytime(long seconds) {
        totalPlaytime += seconds;
    }

    /**
     * Starts a session
     */
    public void startSession() {
        sessionStart = System.currentTimeMillis();
        sessionKills = 0;
    }

    /**
     * Ends a session and records playtime
     */
    public void endSession() {
        if (sessionStart > 0) {
            long duration = (System.currentTimeMillis() - sessionStart) / 1000;
            addPlaytime(duration);
            sessionStart = 0;
        }
    }

    /**
     * Gets or creates arena statistics
     */
    private ArenaStats getArenaStats(String arenaId) {
        return arenaStats.computeIfAbsent(arenaId, k -> new ArenaStats());
    }

    /**
     * Gets arena-specific statistics
     */
    public ArenaStats getArenaStatistics(String arenaId) {
        return arenaStats.getOrDefault(arenaId, new ArenaStats());
    }

    /**
     * Calculates win rate
     */
    public double getWinRate() {
        if (totalAttempts == 0) return 0.0;
        return (double) totalCompletions / totalAttempts * 100.0;
    }

    /**
     * Calculates K/D ratio
     */
    public double getKDRatio() {
        if (totalDeaths == 0) return totalKills;
        return (double) totalKills / totalDeaths;
    }

    // Getters and Setters
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public int getTotalCompletions() {
        return totalCompletions;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public long getTotalPlaytime() {
        return totalPlaytime;
    }

    public int getHighestWave() {
        return highestWave;
    }

    public long getFastestCompletion() {
        return fastestCompletion;
    }

    public int getLongestKillstreak() {
        return longestKillstreak;
    }

    public void setLongestKillstreak(int killstreak) {
        if (killstreak > longestKillstreak) {
            longestKillstreak = killstreak;
        }
    }

    public int getSessionKills() {
        return sessionKills;
    }

    /**
     * Inner class for arena-specific statistics
     */
    public static class ArenaStats {
        public int attempts = 0;
        public int completions = 0;
        public long fastestCompletion = 0;
        public int totalKills = 0;
        public int totalDeaths = 0;

        public double getWinRate() {
            if (attempts == 0) return 0.0;
            return (double) completions / attempts * 100.0;
        }
    }

    @Override
    public String toString() {
        return "PlayerStatistics{" +
                "player=" + playerName +
                ", kills=" + totalKills +
                ", deaths=" + totalDeaths +
                ", completions=" + totalCompletions +
                ", attempts=" + totalAttempts +
                ", winRate=" + String.format("%.1f", getWinRate()) + "%" +
                '}';
    }
}