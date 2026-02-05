package me.bixgamer707.hordes.mob;

import java.util.Objects;

/**
 * Represents a mob configuration for waves
 * Supports both MythicMobs and vanilla mobs
 *
 * Includes health/damage modifiers and custom names
 */
public class HordeMob {

    private final MobType type;
    private final String id;

    // Modifiers
    private double healthMultiplier;
    private double damageMultiplier;
    private String customName;

    // Future extensions
    // private Map<Enchantment, Integer> equipment;
    // private List<PotionEffect> effects;
    // private List<ItemStack> drops;

    public HordeMob(MobType type, String id) {
        this.type = type;
        this.id = id;
        this.healthMultiplier = 1.0;
        this.damageMultiplier = 1.0;
    }

    /**
     * Creates a MythicMobs mob
     *
     * @param mythicId MythicMobs internal ID
     * @return HordeMob instance
     */
    public static HordeMob mythic(String mythicId) {
        return new HordeMob(MobType.MYTHIC, mythicId);
    }

    /**
     * Creates a vanilla mob
     *
     * @param entityType Bukkit EntityType name
     * @return HordeMob instance
     */
    public static HordeMob vanilla(String entityType) {
        return new HordeMob(MobType.VANILLA, entityType);
    }

    /**
     * Sets custom name (builder pattern)
     *
     * @param name Custom name (supports color codes)
     * @return this instance
     */
    public HordeMob withCustomName(String name) {
        this.customName = name;
        return this;
    }

    /**
     * Sets health multiplier (builder pattern)
     *
     * @param multiplier Health multiplier (1.0 = normal, 2.0 = double)
     * @return this instance
     */
    public HordeMob withHealthMultiplier(double multiplier) {
        this.healthMultiplier = Math.max(0.1, multiplier);
        return this;
    }

    /**
     * Sets damage multiplier (builder pattern)
     *
     * @param multiplier Damage multiplier (1.0 = normal, 2.0 = double)
     * @return this instance
     */
    public HordeMob withDamageMultiplier(double multiplier) {
        this.damageMultiplier = Math.max(0.1, multiplier);
        return this;
    }

    /**
     * Checks if this is a MythicMobs mob
     *
     * @return true if MythicMobs
     */
    public boolean isMythicMob() {
        return type == MobType.MYTHIC;
    }

    /**
     * Checks if this is a vanilla mob
     *
     * @return true if vanilla
     */
    public boolean isVanillaMob() {
        return type == MobType.VANILLA;
    }

    // Getters and Setters

    public MobType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    public void setHealthMultiplier(double healthMultiplier) {
        this.healthMultiplier = Math.max(0.1, healthMultiplier);
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public void setDamageMultiplier(double damageMultiplier) {
        this.damageMultiplier = Math.max(0.1, damageMultiplier);
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    @Override
    public String toString() {
        return "HordeMob{" +
                "type=" + type +
                ", id='" + id + '\'' +
                ", health=" + healthMultiplier + "x" +
                ", damage=" + damageMultiplier + "x" +
                (customName != null ? ", name='" + customName + '\'' : "") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HordeMob hordeMob = (HordeMob) o;
        return type == hordeMob.type && id.equals(hordeMob.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }
}