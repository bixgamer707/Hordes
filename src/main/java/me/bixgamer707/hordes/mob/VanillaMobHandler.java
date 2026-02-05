package me.bixgamer707.hordes.mob;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

/**
 * Handles spawning and modification of vanilla Minecraft mobs
 * Supports health and damage multipliers
 */
public class VanillaMobHandler {

    /**
     * Spawns a vanilla mob with modifiers
     * 
     * @param entityTypeName EntityType name (e.g., "ZOMBIE", "SKELETON")
     * @param location Location to spawn at
     * @param healthMultiplier Health multiplier (1.0 = normal)
     * @return Spawned entity or null if failed
     */
    public Entity spawnMob(String entityTypeName, Location location, double healthMultiplier) {
        try {
            // Parse EntityType
            EntityType entityType = EntityType.valueOf(entityTypeName.toUpperCase());
            
            // Validate it's a living entity
            if (!entityType.isAlive()) {
                Bukkit.getLogger().warning("[Hordes] EntityType is not alive: " + entityTypeName);
                return null;
            }
            
            // Spawn the mob
            Entity entity = location.getWorld().spawnEntity(location, entityType);
            
            if (!(entity instanceof LivingEntity)) {
                Bukkit.getLogger().warning("[Hordes] Spawned entity is not LivingEntity: " + entityTypeName);
                entity.remove();
                return null;
            }
            
            LivingEntity living = (LivingEntity) entity;
            
            // Apply health multiplier
            if (healthMultiplier != 1.0) {
                applyHealthMultiplier(living, healthMultiplier);
            }
            
            // Configure persistence
            living.setRemoveWhenFarAway(false);
            
            return entity;
            
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[Hordes] Invalid EntityType: " + entityTypeName);
            return null;
        } catch (Exception e) {
            Bukkit.getLogger().severe("[Hordes] Error spawning vanilla mob: " + entityTypeName);
            // Only print stack trace in debug mode
            return null;
        }
    }

    /**
     * Applies health multiplier to a living entity
     * 
     * @param entity Entity to modify
     * @param multiplier Health multiplier
     */
    public void applyHealthMultiplier(LivingEntity entity, double multiplier) {
        if (multiplier <= 0 || multiplier == 1.0) {
            return;
        }
        
        try {
            AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            
            if (maxHealthAttr == null) {
                return;
            }
            
            // Get base max health
            double baseMaxHealth = maxHealthAttr.getBaseValue();
            
            // Calculate new max health
            double newMaxHealth = baseMaxHealth * multiplier;
            
            // Apply new max health
            maxHealthAttr.setBaseValue(newMaxHealth);
            
            // Heal to new max health
            entity.setHealth(newMaxHealth);
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Hordes] Error applying health multiplier: " + e.getMessage());
        }
    }

    /**
     * Applies damage multiplier to a living entity
     * 
     * @param entity Entity to modify
     * @param multiplier Damage multiplier
     */
    public void applyDamageMultiplier(LivingEntity entity, double multiplier) {
        if (multiplier <= 0 || multiplier == 1.0) {
            return;
        }
        
        try {
            AttributeInstance attackDamageAttr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            
            if (attackDamageAttr == null) {
                // Not all mobs have attack damage (e.g., passive mobs)
                return;
            }
            
            // Get base damage
            double baseDamage = attackDamageAttr.getBaseValue();
            
            // Calculate new damage
            double newDamage = baseDamage * multiplier;
            
            // Apply new damage
            attackDamageAttr.setBaseValue(newDamage);
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Hordes] Error applying damage multiplier: " + e.getMessage());
        }
    }

    /**
     * Applies both health and damage multipliers
     * 
     * @param entity Entity to modify
     * @param healthMultiplier Health multiplier
     * @param damageMultiplier Damage multiplier
     */
    public void applyModifiers(LivingEntity entity, double healthMultiplier, double damageMultiplier) {
        applyHealthMultiplier(entity, healthMultiplier);
        applyDamageMultiplier(entity, damageMultiplier);
    }

    /**
     * Validates if an EntityType name is valid
     * 
     * @param entityTypeName EntityType name to check
     * @return true if valid and alive
     */
    public boolean isValidEntityType(String entityTypeName) {
        try {
            EntityType type = EntityType.valueOf(entityTypeName.toUpperCase());
            return type.isAlive();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Gets all valid living entity types
     * 
     * @return Array of living EntityType names
     */
    public String[] getAllLivingEntityTypes() {
        return java.util.Arrays.stream(EntityType.values())
            .filter(EntityType::isAlive)
            .map(EntityType::name)
            .sorted()
            .toArray(String[]::new);
    }

    /**
     * Spawns multiple mobs at once
     * 
     * @param entityTypeName EntityType name
     * @param location Location to spawn at
     * @param amount Number of mobs to spawn
     * @param healthMultiplier Health multiplier
     * @return List of spawned entities
     */
    public java.util.List<Entity> spawnMobs(String entityTypeName, Location location, int amount, double healthMultiplier) {
        java.util.List<Entity> spawned = new java.util.ArrayList<>();
        
        for (int i = 0; i < amount; i++) {
            Entity entity = spawnMob(entityTypeName, location, healthMultiplier);
            if (entity != null) {
                spawned.add(entity);
            }
        }
        
        return spawned;
    }
}
