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
    private final Map<String, ArenaStats> arenaStats;
    
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
    public void addKill(String arenaId) {
        totalKills++;
        sessionKills++;
        getArenaStatistics(arenaId).setKills(getArenaStatistics(arenaId).getKills()+1);
    }

    /**
     * Records a death
     */
    public void addDeath(String arenaId) {
        totalDeaths++;

        getArenaStatistics(arenaId).setDeaths(getArenaStatistics(arenaId).getDeaths()+1);

        sessionKills = 0; // Reset killstreak
    }

    /**
     * Records an arena attempt
     */
    public void addAttempt(String arenaId) {
        totalAttempts++;
        getArenaStats(arenaId).setAttempts(getArenaStatistics(arenaId).getAttempts()+1);
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
        ArenaStats stats = getArenaStats(arenaId);
        // Update arena stats
        stats.setCompletions(stats.completions++);
        if (stats.fastestCompletion == 0 || duration < stats.fastestCompletion) {
            stats.setFastestCompletion(duration);
        }

        if(stats.getHighestWave() < wavesCompleted) {
            stats.setHighestWave(wavesCompleted);
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
    public void endSession(String arenaId) {
        if (sessionStart > 0) {
            long duration = (System.currentTimeMillis() - sessionStart) / 1000;
            addPlaytime(duration);

            getArenaStatistics(arenaId).setPlayTime(getArenaStatistics(arenaId).getPlayTime()+duration);
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

    public void setTotalKills(int kills) {
        this.totalKills = kills;
    }

    public void setTotalDeaths(int deaths) {
        this.totalDeaths = deaths;
    }

    public void setTotalCompletions(int completions) {
        this.totalCompletions = completions;
    }

    public void setTotalAttempts(int attempts) {
        this.totalAttempts = attempts;
    }

    public void setTotalPlaytime(long playtime) {
        this.totalPlaytime = playtime;
    }

    public void setHighestWave(int highestWave) {
        this.highestWave = highestWave;
    }

    public void setFastestCompletion(long fastestCompletion) {
        this.fastestCompletion = fastestCompletion;
    }

    public void setArenaStats(ArenaStats stats, String arenaId) {
        this.arenaStats.put(arenaId, stats);
    }

    /**
     * Inner class for arena-specific statistics
     */
    public static class ArenaStats {
        public int attempts = 0;
        public int completions = 0;
        public long fastestCompletion = 0;
        public long playTime = 0;
        public int totalKills = 0;
        public int highestWave = 0;
        public int totalDeaths = 0;

        public double getWinRate() {
            if (attempts == 0) return 0.0;
            return (double) completions / attempts * 100.0;
        }

        public double getKDRatio() {
            if (totalDeaths == 0) return totalKills;
            return (double) totalKills / totalDeaths;
        }

        public long getFastestCompletion() {
            return fastestCompletion;
        }

        public void setFastestCompletion(long duration) {
            if (fastestCompletion == 0 || duration < fastestCompletion) {
                fastestCompletion = duration;
            }
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        public void setCompletions(int completions) {
            this.completions = completions;
        }

        public void setKills(int kills) {
            this.totalKills = kills;
        }

        public void setDeaths(int deaths) {
            this.totalDeaths = deaths;
        }


        public int getKills() {
            return totalKills;
        }

        public int getDeaths() {
            return totalDeaths;
        }

        public int getAttempts() {
            return attempts;
        }

        public int getCompletions() {
            return completions;
        }

        public long getPlayTime() {
            return playTime;
        }

        public int getHighestWave() {
            return highestWave;
        }

        public void setHighestWave(int highestWave) {
            this.highestWave = highestWave;
        }

        public void setPlayTime(long playTime) {
            this.playTime = playTime;
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