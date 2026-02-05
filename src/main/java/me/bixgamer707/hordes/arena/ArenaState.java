package me.bixgamer707.hordes.arena;

/**
 * Represents the lifecycle states of an arena
 */
public enum ArenaState {
    
    /**
     * Arena is disabled via configuration
     */
    DISABLED("Disabled"),
    
    /**
     * Waiting for players in lobby
     */
    WAITING("Waiting"),
    
    /**
     * Countdown active before start
     */
    STARTING("Starting"),
    
    /**
     * Arena is active with ongoing waves
     */
    ACTIVE("Active"),
    
    /**
     * Arena is ending (rewards, cleanup)
     */
    ENDING("Ending"),
    
    /**
     * Arena is resetting to initial state
     */
    RESTARTING("Restarting");
    
    private final String displayName;
    
    ArenaState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Checks if arena accepts new players
     */
    public boolean isJoinable() {
        return this == WAITING || this == STARTING;
    }
    
    /**
     * Checks if arena is currently running
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if arena is in termination process
     */
    public boolean isEnding() {
        return this == ENDING || this == RESTARTING;
    }
    
    /**
     * Checks if arena is operational (not disabled)
     */
    public boolean isOperational() {
        return this != DISABLED;
    }
}
