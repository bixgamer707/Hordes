package me.bixgamer707.hordes.config;

/**
 * Tipo de distribución de recompensas
 */
public enum RewardType {
    /**
     * Solo al completar todas las waves
     */
    COMPLETION_ONLY("Only complete"),
    
    /**
     * Recompensa por cada wave + bonus final
     */
    PROGRESSIVE("Progressive"),
    
    /**
     * Ambas opciones (progresivo + bonus final)
     */
    BOTH("Both");
    
    private final String displayName;
    
    RewardType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}