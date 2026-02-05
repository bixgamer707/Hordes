package me.bixgamer707.hordes.player;

/**
 * Represents a player's state within an arena
 */
public enum PlayerState {
    
    /**
     * Player is in the lobby waiting for start
     */
    LOBBY("Lobby"),
    
    /**
     * Player is actively participating in the arena
     */
    PLAYING("Playing"),
    
    /**
     * Player has died and is out of the game
     */
    DEAD("Dead"),
    
    /**
     * Player is spectating the arena
     */
    SPECTATING("Spectating");
    
    private final String displayName;
    
    PlayerState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Checks if player can actively participate
     */
    public boolean canParticipate() {
        return this == PLAYING;
    }
    
    /**
     * Checks if player is alive
     */
    public boolean isAlive() {
        return this == LOBBY || this == PLAYING;
    }
    
    /**
     * Checks if player is inactive
     */
    public boolean isInactive() {
        return this == DEAD || this == SPECTATING;
    }
}
