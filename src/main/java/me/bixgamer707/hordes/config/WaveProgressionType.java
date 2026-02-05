package me.bixgamer707.hordes.config;

/**
 * Tipo de progresión entre waves
 */
public enum WaveProgressionType {
    /**
     * Automático al matar todos los mobs
     */
    AUTOMATIC("Automatic"),
    
    /**
     * Requiere interacción (botón, placa, NPC)
     */
    MANUAL("Manual"),
    
    /**
     * Configurable por arena (puede ser automático en algunas waves, manual en otras)
     */
    MIXED("Mixed");
    
    private final String displayName;
    
    WaveProgressionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}