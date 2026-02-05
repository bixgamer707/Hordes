package me.bixgamer707.hordes.wave;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.config.ArenaConfig;
import me.bixgamer707.hordes.file.File;
import me.bixgamer707.hordes.mob.HordeMob;
import me.bixgamer707.hordes.mob.MobType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages wave creation and configuration for an arena
 * Caches wave configurations for performance
 *
 * Thread-safe implementation
 */
public class WaveManager {

    private final Arena arena;
    private final ArenaConfig arenaConfig;
    private final Hordes plugin;

    // Cached wave configurations: WaveNumber -> WaveConfig
    private final Map<Integer, WaveConfig> waveConfigs;

    // Cache status
    private boolean loaded;

    public WaveManager(Arena arena, ArenaConfig arenaConfig) {
        this.arena = arena;
        this.arenaConfig = arenaConfig;
        this.plugin = arena.getPlugin();
        this.waveConfigs = new HashMap<>();
        this.loaded = false;

        loadWaveConfigs();
    }

    /**
     * Loads all wave configurations from mobs.yml
     * Caches results for performance
     */
    private void loadWaveConfigs() {
        File mobsFile = plugin.getFileManager().getMobs();
        String arenaId = arena.getId();

        ConfigurationSection arenaSection = mobsFile.getConfigurationSection(arenaId);

        if (arenaSection == null) {
            Bukkit.getLogger().warning("[Hordes] No mob configuration found for arena: " + arenaId);
            return;
        }

        int maxWaves = arenaConfig.getTotalWaves();
        int loadedWaves = 0;

        for (int i = 1; i <= maxWaves; i++) {
            WaveConfig config = loadWaveConfig(arenaSection, i);

            if (config != null) {
                waveConfigs.put(i, config);
                loadedWaves++;
            }
        }

        loaded = true;

        if (loadedWaves < maxWaves) {
            Bukkit.getLogger().warning("[Hordes] Arena " + arenaId + " has " + maxWaves +
                    " configured waves but only " + loadedWaves + " mob configurations");
        } else {
            Bukkit.getLogger().info("[Hordes] Loaded " + loadedWaves + " waves for arena " + arenaId);
        }
    }

    /**
     * Loads configuration for a specific wave
     *
     * @param arenaSection Arena configuration section
     * @param waveNumber Wave number to load
     * @return WaveConfig or null if not found
     */
    private WaveConfig loadWaveConfig(ConfigurationSection arenaSection, int waveNumber) {
        // Try "wave-X" format first
        String wavePath = "wave-" + waveNumber;
        ConfigurationSection waveSection = arenaSection.getConfigurationSection(wavePath);

        // Try "waves.X" format if not found
        if (waveSection == null) {
            wavePath = "waves." + waveNumber;
            waveSection = arenaSection.getConfigurationSection(wavePath);
        }

        if (waveSection == null) {
            return null;
        }

        WaveConfig config = new WaveConfig(waveNumber);

        // Load spawn settings
        config.setSpawnDelay(waveSection.getInt("spawn-delay", 20));
        config.setMobsPerSpawn(waveSection.getInt("mobs-per-spawn", 1));

        // Load spawn locations
        List<Location> spawnLocations = loadSpawnLocations(waveSection);
        config.setSpawnLocations(spawnLocations);

        // Load mobs
        List<HordeMob> mobs = loadMobs(waveSection);
        config.setMobs(mobs);

        // Load progression type (for MIXED mode)
        String progressionType = waveSection.getString("progression", "AUTO");
        config.setManualProgression("MANUAL".equalsIgnoreCase(progressionType));

        return config;
    }

    /**
     * Loads spawn locations from configuration
     *
     * @param waveSection Wave configuration section
     * @return List of spawn locations
     */
    private List<Location> loadSpawnLocations(ConfigurationSection waveSection) {
        List<Location> locations = new ArrayList<>();

        if (!waveSection.contains("spawn-locations")) {
            return locations;
        }

        List<Map<?, ?>> locationMaps = waveSection.getMapList("spawn-locations");

        for (Map<?, ?> locMap : locationMaps) {
            try {
                Location loc = parseLocation(locMap);
                if (loc != null) {
                    locations.add(loc);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Hordes] Error loading spawn location: " + e.getMessage());
            }
        }

        return locations;
    }

    /**
     * Parses a location from a map
     *
     * @param locMap Location map
     * @return Location or null if invalid
     */
    private Location parseLocation(Map<?, ?> locMap) {
        String worldName = (String) locMap.get("world");

        if (worldName == null) {
            return null;
        }

        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Bukkit.getLogger().warning("[Hordes] World not found: " + worldName);
            return null;
        }

        double x = getDouble(locMap, "x", 0);
        double y = getDouble(locMap, "y", 64);
        double z = getDouble(locMap, "z", 0);
        float yaw = getFloat(locMap, "yaw", 0);
        float pitch = getFloat(locMap, "pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Loads mobs from configuration
     *
     * @param waveSection Wave configuration section
     * @return List of HordeMobs
     */
    private List<HordeMob> loadMobs(ConfigurationSection waveSection) {
        List<HordeMob> mobs = new ArrayList<>();

        if (!waveSection.contains("mobs")) {
            Bukkit.getLogger().warning("[Hordes] Wave has no mobs configured");
            return mobs;
        }

        List<Map<?, ?>> mobList = waveSection.getMapList("mobs");

        for (Map<?, ?> mobMap : mobList) {
            try {
                List<HordeMob> parsedMobs = parseMob(mobMap);
                mobs.addAll(parsedMobs);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Hordes] Error loading mob: " + e.getMessage());
            }
        }

        return mobs;
    }

    /**
     * Parses a mob configuration
     *
     * @param mobMap Mob configuration map
     * @return List of HordeMobs (amount may create multiple)
     */
    private List<HordeMob> parseMob(Map<?, ?> mobMap) {
        List<HordeMob> mobs = new ArrayList<>();

        // Get type
        String typeStr = (String) mobMap.get("type");

        if(typeStr == null){
            typeStr = "VANILLA";
        }
        MobType type;

        try {
            type = MobType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[Hordes] Invalid mob type: " + typeStr);
            type = MobType.VANILLA;
        }

        // Get ID
        String id = (String) mobMap.get("id");
        if (id == null) {
            Bukkit.getLogger().warning("[Hordes] Mob has no ID specified");
            return mobs;
        }

        // Get amount
        int amount = getInt(mobMap, "amount", 1);

        // Get modifiers
        double healthMultiplier = getDouble(mobMap, "health-multiplier", 1.0);
        double damageMultiplier = getDouble(mobMap, "damage-multiplier", 1.0);
        String customName = (String) mobMap.get("custom-name");

        // Create mobs
        for (int i = 0; i < amount; i++) {
            HordeMob mob = new HordeMob(type, id);
            mob.setHealthMultiplier(healthMultiplier);
            mob.setDamageMultiplier(damageMultiplier);

            if (customName != null) {
                mob.setCustomName(customName);
            }

            mobs.add(mob);
        }

        return mobs;
    }

    /**
     * Creates a wave instance
     *
     * @param waveNumber Wave number to create
     * @return Wave or null if configuration not found
     */
    public Wave createWave(int waveNumber) {
        WaveConfig config = waveConfigs.get(waveNumber);

        if (config == null) {
            return null;
        }

        return new Wave(arena, waveNumber, config);
    }

    /**
     * Gets total number of configured waves
     *
     * @return Wave count
     */
    public int getTotalWaves() {
        return waveConfigs.size();
    }

    /**
     * Checks if a wave exists
     *
     * @param waveNumber Wave number
     * @return true if wave is configured
     */
    public boolean hasWave(int waveNumber) {
        return waveConfigs.containsKey(waveNumber);
    }

    /**
     * Gets wave configuration
     *
     * @param waveNumber Wave number
     * @return WaveConfig or null
     */
    public WaveConfig getWaveConfig(int waveNumber) {
        return waveConfigs.get(waveNumber);
    }

    /**
     * Checks if configurations are loaded
     *
     * @return true if loaded
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Reloads all wave configurations
     */
    public void reload() {
        waveConfigs.clear();
        loaded = false;
        loadWaveConfigs();
    }

    // Utility methods for safe type conversion

    private int getInt(Map<?, ?> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private double getDouble(Map<?, ?> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private float getFloat(Map<?, ?> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }
}