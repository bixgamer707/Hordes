package me.bixgamer707.hordes.mob;

/**
 * Tipos de mobs soportados por el plugin
 */
public enum MobType {

    /**
     * Mob de MythicMobs
     */
    MYTHIC("MythicMobs"),

    /**
     * Mob vanilla de Minecraft
     */
    VANILLA("Vanilla");

    private final String displayName;

    MobType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}