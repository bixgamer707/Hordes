package me.bixgamer707.hordes.config;

/**
 * Modo de drop de items cuando un jugador muere
 */
public enum ItemDropMode {
    /**
     * Cualquier jugador puede recoger los items
     */
    ALL_PLAYERS("All"),
    
    /**
     * Solo jugadores de la misma arena pueden recoger
     */
    ARENA_PLAYERS("Arena"),
    
    /**
     * Solo el jugador muerto puede recoger (cuando respawnea)
     */
    OWNER_ONLY("Owner"),
    
    /**
     * Items se teletransportan con el jugador al salir
     */
    TELEPORT_WITH_PLAYER("Teleport");
    
    private final String displayName;
    
    ItemDropMode(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}