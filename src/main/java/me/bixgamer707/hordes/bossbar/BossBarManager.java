package me.bixgamer707.hordes.bossbar;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.wave.Wave;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages boss bars for arena wave progress
 * Shows current wave and mob count
 */
public class BossBarManager {

    private final Hordes plugin;
    
    // Arena ID -> BossBar
    private final Map<String, BossBar> arenaBars;
    
    // Player UUID -> Arena ID
    private final Map<UUID, String> playerBars;

    public BossBarManager(Hordes plugin) {
        this.plugin = plugin;
        this.arenaBars = new HashMap<>();
        this.playerBars = new HashMap<>();
    }

    /**
     * Creates or updates boss bar for arena
     */
    public void updateBossBar(Arena arena) {
        String arenaId = arena.getId();
        BossBar bar = arenaBars.get(arenaId);
        
        // Create if doesn't exist
        if (bar == null) {
            bar = createBossBar(arena);
            arenaBars.put(arenaId, bar);
        }
        
        // Update bar
        updateBarContent(bar, arena);
    }

    /**
     * Creates a boss bar for an arena
     */
    private BossBar createBossBar(Arena arena) {
        String title = getBarTitle(arena);
        BarColor color = getBarColor(arena);
        
        return Bukkit.createBossBar(
            title,
            color,
            BarStyle.SEGMENTED_10
        );
    }

    /**
     * Updates boss bar content
     */
    private void updateBarContent(BossBar bar, Arena arena) {
        // Update title
        bar.setTitle(getBarTitle(arena));
        
        // Update progress
        bar.setProgress(getProgress(arena));
        
        // Update color
        bar.setColor(getBarColor(arena));
    }

    /**
     * Gets boss bar title
     */
    private String getBarTitle(Arena arena) {
        Wave currentWave = arena.getCurrentWave();
        
        if (currentWave == null) {
            return "§e§lWaiting for wave...";
        }
        
        int waveNum = arena.getCurrentWaveNumber();
        int totalWaves = arena.getConfig().getTotalWaves();
        int mobsAlive = currentWave.getMobsAlive();
        
        return String.format("§e§lWave %d/%d §7- §c%d §7mobs remaining", 
            waveNum, totalWaves, mobsAlive);
    }

    /**
     * Calculates progress (0.0 to 1.0)
     */
    private double getProgress(Arena arena) {
        Wave currentWave = arena.getCurrentWave();
        
        if (currentWave == null) {
            return 1.0;
        }
        
        int mobsAlive = currentWave.getMobsAlive();
        int totalMobs = currentWave.getTotalMobs();
        
        if (totalMobs == 0) {
            return 1.0;
        }
        
        // Progress is inverse (full bar = all alive, empty = all dead)
        return (double) mobsAlive / totalMobs;
    }

    /**
     * Gets bar color based on progress
     */
    private BarColor getBarColor(Arena arena) {
        Wave currentWave = arena.getCurrentWave();
        
        if (currentWave == null) {
            return BarColor.YELLOW;
        }
        
        int mobsAlive = currentWave.getMobsAlive();
        int totalMobs = currentWave.getTotalMobs();
        
        if (totalMobs == 0) {
            return BarColor.GREEN;
        }
        
        double percentage = (double) mobsAlive / totalMobs;
        
        if (percentage > 0.6) {
            return BarColor.RED;      // Many mobs left
        } else if (percentage > 0.3) {
            return BarColor.YELLOW;   // Medium
        } else {
            return BarColor.GREEN;    // Almost done
        }
    }

    /**
     * Shows boss bar to player
     */
    public void showBossBar(Player player, String arenaId) {
        BossBar bar = arenaBars.get(arenaId);
        
        if (bar != null && !bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
            playerBars.put(player.getUniqueId(), arenaId);
        }
    }

    /**
     * Hides boss bar from player
     */
    public void hideBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        String arenaId = playerBars.remove(uuid);
        
        if (arenaId != null) {
            BossBar bar = arenaBars.get(arenaId);
            if (bar != null) {
                bar.removePlayer(player);
            }
        }
    }

    /**
     * Removes boss bar for arena
     */
    public void removeBossBar(String arenaId) {
        BossBar bar = arenaBars.remove(arenaId);
        
        if (bar != null) {
            bar.removeAll();
        }
        
        // Remove player mappings
        playerBars.entrySet().removeIf(entry -> entry.getValue().equals(arenaId));
    }

    /**
     * Cleans up all boss bars
     */
    public void cleanup() {
        for (BossBar bar : arenaBars.values()) {
            bar.removeAll();
        }
        
        arenaBars.clear();
        playerBars.clear();
    }
}