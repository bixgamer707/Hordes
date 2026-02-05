package me.bixgamer707.hordes.mob;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.bixgamer707.hordes.Hordes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Integration with MythicMobs plugin
 * Handles spawning and identification of MythicMobs entities
 *
 * Gracefully degrades if MythicMobs is not available
 */
public class MythicMobsIntegration {

    private final Hordes plugin;
    private boolean enabled;

    public MythicMobsIntegration(Hordes plugin) {
        this.plugin = plugin;
        this.enabled = false;

        checkMythicMobs();
    }

    /**
     * Checks if MythicMobs is available and loads API
     */
    private void checkMythicMobs() {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            plugin.logInfo("MythicMobs not found - only vanilla mobs will be available");
            enabled = false;
            return;
        }

        try {
            // Test API access
            MythicBukkit.inst().getMobManager();
            enabled = true;
            plugin.logInfo("MythicMobs integration enabled successfully");
        } catch (Exception e) {
            plugin.logWarning("MythicMobs found but API failed to load: " + e.getMessage());
            enabled = false;
        }
    }

    /**
     * Spawns a MythicMobs mob
     *
     * @param mythicId MythicMobs internal ID
     * @param location Location to spawn at
     * @return Spawned entity or null if failed
     */
    public Entity spawnMob(String mythicId, Location location) {
        if (!enabled) {
            plugin.logWarning("Attempted to spawn MythicMob but integration is disabled");
            return null;
        }

        try {
            // Spawn using MythicMobs API
            ActiveMob activeMob = MythicBukkit.inst()
                    .getMobManager()
                    .spawnMob(mythicId, location);

            if (activeMob != null) {
                Entity entity = activeMob.getEntity().getBukkitEntity();

                if (entity != null) {
                    return entity;
                } else {
                    plugin.logWarning("MythicMob spawned but entity is null: " + mythicId);
                }
            } else {
                plugin.logWarning("Failed to spawn MythicMob (does it exist?): " + mythicId);
            }
        } catch (Exception e) {
            plugin.logError("Error spawning MythicMob '" + mythicId + "': " + e.getMessage());
            if (plugin.getFileManager().getConfig().getBoolean("settings.debug", false)) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Checks if an entity is a MythicMob
     *
     * @param entity Entity to check
     * @return true if it's a MythicMob
     */
    public boolean isMythicMob(Entity entity) {
        if (!enabled || entity == null) {
            return false;
        }

        try {
            return MythicBukkit.inst().getMobManager().isMythicMob(entity);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the MythicMobs internal name of a mob
     *
     * @param entity Entity to check
     * @return Internal name or null if not a MythicMob
     */
    public String getMythicMobType(Entity entity) {
        if (!enabled || !isMythicMob(entity)) {
            return null;
        }

        try {
            ActiveMob activeMob = MythicBukkit.inst()
                    .getMobManager()
                    .getActiveMob(entity.getUniqueId())
                    .orElse(null);

            if (activeMob != null) {
                return activeMob.getType().getInternalName();
            }
        } catch (Exception e) {
            plugin.logWarning("Error getting MythicMob type: " + e.getMessage());
        }

        return null;
    }

    /**
     * Gets the ActiveMob instance for an entity
     *
     * @param entity Entity to get ActiveMob for
     * @return ActiveMob or null
     */
    public ActiveMob getActiveMob(Entity entity) {
        if (!enabled || entity == null) {
            return null;
        }

        try {
            return MythicBukkit.inst()
                    .getMobManager()
                    .getActiveMob(entity.getUniqueId())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if a MythicMob type exists
     *
     * @param mythicId MythicMobs internal ID
     * @return true if mob type exists
     */
    public boolean mobTypeExists(String mythicId) {
        if (!enabled) {
            return false;
        }

        try {
            return MythicBukkit.inst()
                    .getMobManager()
                    .getMythicMob(mythicId)
                    .isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets all registered MythicMob types
     *
     * @return Set of mob type names
     */
    public java.util.Set<String> getAllMobTypes() {
        if (!enabled) {
            return java.util.Collections.emptySet();
        }

        try {
            return MythicBukkit.inst()
                    .getMobManager()
                    .getMobTypes()
                    .stream()
                    .map(type -> type.getInternalName())
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            plugin.logWarning("Error getting MythicMob types: " + e.getMessage());
            return java.util.Collections.emptySet();
        }
    }

    /**
     * Checks if integration is enabled
     *
     * @return true if MythicMobs is available
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Reloads the integration
     */
    public void reload() {
        checkMythicMobs();
    }
}