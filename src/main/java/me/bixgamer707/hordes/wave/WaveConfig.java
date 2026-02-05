package me.bixgamer707.hordes.wave;

import me.bixgamer707.hordes.mob.HordeMob;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for a single wave
 * Contains mob list, spawn settings, and progression type
 * 
 * Immutable after configuration for thread-safety
 */
public class WaveConfig {

    private final int waveNumber;
    
    // Spawn settings
    private int spawnDelay;
    private int mobsPerSpawn;
    
    // Progression
    private boolean manualProgression;
    
    // Mobs to spawn
    private List<HordeMob> mobs;
    
    // Spawn locations (optional)
    private List<Location> spawnLocations;
    
    public WaveConfig(int waveNumber) {
        this.waveNumber = waveNumber;
        this.spawnDelay = 20;
        this.mobsPerSpawn = 1;
        this.manualProgression = false;
        this.mobs = new ArrayList<>();
        this.spawnLocations = new ArrayList<>();
    }

    /**
     * Adds a mob to this wave
     * 
     * @param mob Mob to add
     */
    public void addMob(HordeMob mob) {
        if (mob != null) {
            this.mobs.add(mob);
        }
    }

    /**
     * Adds multiple mobs
     * 
     * @param mobs List of mobs to add
     */
    public void addMobs(List<HordeMob> mobs) {
        if (mobs != null) {
            this.mobs.addAll(mobs);
        }
    }

    /**
     * Adds a spawn location
     * 
     * @param location Location to add
     */
    public void addSpawnLocation(Location location) {
        if (location != null) {
            this.spawnLocations.add(location);
        }
    }

    /**
     * Gets total mob count
     * 
     * @return Number of mobs in this wave
     */
    public int getTotalMobs() {
        return mobs.size();
    }

    /**
     * Checks if wave has custom spawn locations
     * 
     * @return true if custom locations are set
     */
    public boolean hasCustomSpawnLocations() {
        return !spawnLocations.isEmpty();
    }

    /**
     * Checks if this wave requires manual progression
     * 
     * @return true if manual progression required
     */
    public boolean requiresManualProgression() {
        return manualProgression;
    }

    // Getters and Setters
    
    public int getWaveNumber() {
        return waveNumber;
    }

    public int getSpawnDelay() {
        return spawnDelay;
    }

    public void setSpawnDelay(int spawnDelay) {
        this.spawnDelay = Math.max(1, spawnDelay);
    }

    public int getMobsPerSpawn() {
        return mobsPerSpawn;
    }

    public void setMobsPerSpawn(int mobsPerSpawn) {
        this.mobsPerSpawn = Math.max(1, mobsPerSpawn);
    }

    public boolean isManualProgression() {
        return manualProgression;
    }

    public void setManualProgression(boolean manualProgression) {
        this.manualProgression = manualProgression;
    }

    /**
     * Gets copy of mobs list (defensive copy)
     * 
     * @return List of mobs
     */
    public List<HordeMob> getMobs() {
        return new ArrayList<>(mobs);
    }

    public void setMobs(List<HordeMob> mobs) {
        this.mobs = mobs != null ? new ArrayList<>(mobs) : new ArrayList<>();
    }

    /**
     * Gets copy of spawn locations (defensive copy)
     * 
     * @return List of spawn locations
     */
    public List<Location> getSpawnLocations() {
        return new ArrayList<>(spawnLocations);
    }

    public void setSpawnLocations(List<Location> spawnLocations) {
        this.spawnLocations = spawnLocations != null ? new ArrayList<>(spawnLocations) : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "WaveConfig{" +
                "wave=" + waveNumber +
                ", mobs=" + mobs.size() +
                ", spawnDelay=" + spawnDelay +
                ", mobsPerSpawn=" + mobsPerSpawn +
                ", manual=" + manualProgression +
                '}';
    }
}