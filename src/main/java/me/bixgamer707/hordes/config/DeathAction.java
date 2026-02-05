package me.bixgamer707.hordes.config;

/**
 * Acción a tomar cuando un jugador muere
 */
public enum DeathAction {
    /**
     * Kickear del dungeon/arena (no puede volver)
     */
    KICK("Kick"),
    
    /**
     * Modo espectador (puede ver pero no participar)
     */
    SPECTATE("Spectate"),
    
    /**
     * Puede re-entrar si el dungeon sigue activo
     */
    REJOIN("Rejoin"),
    
    /**
     * Respawn en el dungeon (con penalización opcional)
     */
    RESPAWN("Respawn");
    
    private final String displayName;
    
    DeathAction(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}