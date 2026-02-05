package me.bixgamer707.hordes.wave;

import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.mob.HordeMob;
import me.bixgamer707.hordes.mob.MobType;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a single wave in an arena
 * Handles mob spawning, tracking, and completion
 *
 * Thread-safe implementation with proper cleanup
 */
public class Wave {

    private final Arena arena;
    private final int waveNumber;
    private final WaveConfig config;

    // Mob tracking
    private final List<HordeMob> mobsToSpawn;
    private final Set<UUID> spawnedMobs;
    private int mobsAlive;
    private final int totalMobs;

    // State
    private WaveState state;
    private BukkitTask spawnTask;
    private long startTime;

    public Wave(Arena arena, int waveNumber, WaveConfig config) {
        this.arena = arena;
        this.waveNumber = waveNumber;
        this.config = config;

        this.mobsToSpawn = new ArrayList<>(config.getMobs());
        this.spawnedMobs = new HashSet<>();
        this.totalMobs = mobsToSpawn.size();
        this.mobsAlive = 0;

        this.state = WaveState.PENDING;
    }

    /**
     * Starts the wave
     * Begins gradual mob spawning
     */
    public void start() {
        if (state != WaveState.PENDING) {
            return;
        }

        state = WaveState.SPAWNING;
        startTime = System.currentTimeMillis();

        // Broadcast wave start
        arena.broadcastMessage("wave.starting", waveNumber, totalMobs);

        // Start spawning mobs
        startSpawning();
    }

    /**
     * Starts gradual mob spawning
     */
    private void startSpawning() {
        final int spawnDelay = config.getSpawnDelay(); // ticks between spawns
        final int mobsPerSpawn = config.getMobsPerSpawn(); // mobs per cycle

        spawnTask = new BukkitRunnable() {
            int spawnedCount = 0;

            @Override
            public void run() {
                // Check if all mobs spawned
                if (spawnedCount >= mobsToSpawn.size()) {
                    cancel();
                    state = WaveState.ACTIVE;
                    arena.broadcastMessage("arena.all-spawned", waveNumber);
                    return;
                }

                // Spawn next batch
                int toSpawn = Math.min(mobsPerSpawn, mobsToSpawn.size() - spawnedCount);

                for (int i = 0; i < toSpawn; i++) {
                    if (spawnedCount >= mobsToSpawn.size()) break;

                    HordeMob mobConfig = mobsToSpawn.get(spawnedCount);
                    spawnMob(mobConfig);
                    spawnedCount++;
                }
            }
        }.runTaskTimer(arena.getPlugin(), 0L, spawnDelay);
    }

    /**
     * Spawns a single mob
     */
    private void spawnMob(HordeMob mobConfig) {
        Location spawnLocation = getRandomSpawnLocation();

        if (spawnLocation == null) {
            arena.getPlugin().logWarning("No spawn location found for wave " + waveNumber);
            return;
        }

        Entity entity = null;

        // Spawn based on type
        if (mobConfig.getType() == MobType.MYTHIC) {
            // MythicMobs
            entity = arena.getPlugin().getMythicMobsIntegration()
                    .spawnMob(mobConfig.getId(), spawnLocation);
        } else {
            // Vanilla mob
            entity = arena.getPlugin().getVanillaMobHandler()
                    .spawnMob(
                            mobConfig.getId(),
                            spawnLocation,
                            mobConfig.getHealthMultiplier()
                    );
        }

        if (entity == null) {
            arena.getPlugin().logWarning("Failed to spawn mob: " + mobConfig.getId());
            return;
        }

        // Apply damage multiplier if vanilla
        if (mobConfig.getType() == MobType.VANILLA &&
                entity instanceof org.bukkit.entity.LivingEntity) {
            arena.getPlugin().getVanillaMobHandler().applyDamageMultiplier(
                    (org.bukkit.entity.LivingEntity) entity,
                    mobConfig.getDamageMultiplier()
            );
        }

        // Mark entity with metadata
        entity.setMetadata("hordes_arena",
                new FixedMetadataValue(arena.getPlugin(), arena.getId()));
        entity.setMetadata("hordes_wave",
                new FixedMetadataValue(arena.getPlugin(), waveNumber));
        entity.setMetadata("hordes_mob_id",
                new FixedMetadataValue(arena.getPlugin(), mobConfig.getId()));

        // Apply custom name if configured
        if (mobConfig.getCustomName() != null) {
            String name = Text.createText(mobConfig.getCustomName()).build();
            entity.setCustomName(name);
            entity.setCustomNameVisible(true);
        }

        // Prevent despawning
        if (entity instanceof org.bukkit.entity.LivingEntity) {
            ((org.bukkit.entity.LivingEntity) entity).setRemoveWhenFarAway(false);
        }

        // Track the mob
        spawnedMobs.add(entity.getUniqueId());
        mobsAlive++;
    }

    /**
     * Gets a random spawn location for mobs
     */
    private Location getRandomSpawnLocation() {
        List<Location> spawnPoints = config.getSpawnLocations();

        // Use custom spawn points if configured
        if (spawnPoints != null && !spawnPoints.isEmpty()) {
            int randomIndex = ThreadLocalRandom.current().nextInt(spawnPoints.size());
            return spawnPoints.get(randomIndex).clone();
        }

        // Fallback to arena spawn
        return arena.getConfig().getArenaSpawn().clone();
    }

    /**
     * Called when a mob from this wave dies
     *
     * @param mobUuid UUID of the dead mob
     */
    public void onMobDeath(UUID mobUuid) {
        // Check if this mob belongs to this wave
        if (!spawnedMobs.contains(mobUuid)) {
            return;
        }

        mobsAlive--;

        // Broadcast progress at intervals
        if (mobsAlive > 0 && (mobsAlive % 5 == 0 || mobsAlive <= 3)) {
            arena.broadcastMessage("arena.progress", mobsAlive, totalMobs);
        }

        // Check for completion
        if (mobsAlive <= 0 && state == WaveState.ACTIVE) {
            complete();
        }
    }

    /**
     * Completes the wave
     */
    private void complete() {
        state = WaveState.COMPLETED;

        long duration = (System.currentTimeMillis() - startTime) / 1000;

        // Broadcast completion
        arena.broadcastMessage("wave.completed", waveNumber, duration);

        // Cancel spawn task if still running
        if (spawnTask != null && !spawnTask.isCancelled()) {
            spawnTask.cancel();
        }

        // Notify arena
        arena.onWaveComplete();
    }

    /**
     * Cleans up all mobs from this wave
     * Called when arena ends prematurely
     */
    public void cleanup() {
        state = WaveState.CANCELLED;

        // Cancel spawn task
        if (spawnTask != null && !spawnTask.isCancelled()) {
            spawnTask.cancel();
        }

        // Remove all spawned mobs
        for (UUID mobUuid : new HashSet<>(spawnedMobs)) {
            Entity entity = Bukkit.getEntity(mobUuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }

        spawnedMobs.clear();
        mobsAlive = 0;
    }

    /**
     * Checks if a mob belongs to this wave
     *
     * @param mobUuid Mob UUID
     * @return true if mob is from this wave
     */
    public boolean isMobFromWave(UUID mobUuid) {
        return spawnedMobs.contains(mobUuid);
    }

    /**
     * Gets debug information
     *
     * @return Debug string
     */
    public String getDebugInfo() {
        return String.format("Wave %d | State: %s | Alive: %d/%d | Spawned: %d",
                waveNumber, state.name(), mobsAlive, totalMobs, spawnedMobs.size());
    }

    // Getters

    public int getWaveNumber() {
        return waveNumber;
    }

    public WaveState getState() {
        return state;
    }

    public int getMobsAlive() {
        return mobsAlive;
    }

    public int getTotalMobs() {
        return totalMobs;
    }

    public Set<UUID> getSpawnedMobs() {
        return new HashSet<>(spawnedMobs);
    }

    public boolean isActive() {
        return state == WaveState.SPAWNING || state == WaveState.ACTIVE;
    }

    public boolean isCompleted() {
        return state == WaveState.COMPLETED;
    }

    public boolean isCancelled() {
        return state == WaveState.CANCELLED;
    }
}