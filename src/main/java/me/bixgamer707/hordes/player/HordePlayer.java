package me.bixgamer707.hordes.player;

import me.bixgamer707.hordes.arena.Arena;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.UUID;

/**
 * Wrapper for a player participating in an arena
 * Manages state saving/restoration and statistics
 * 
 * Optimized for minimal memory footprint and fast state restoration
 */
public class HordePlayer {

    private final UUID uuid;
    private final Arena arena;
    
    // Original state (for restoration)
    private SavedPlayerState savedState;
    
    // Current state
    private PlayerState state;
    
    // Session statistics
    private int kills;
    private int deaths;
    private long joinTime;
    private long deathTime;

    public HordePlayer(UUID uuid, Arena arena) {
        this.uuid = uuid;
        this.arena = arena;
        this.state = PlayerState.LOBBY;
        this.kills = 0;
        this.deaths = 0;
        this.joinTime = System.currentTimeMillis();
        this.deathTime = 0;
    }

    /**
     * Alternate constructor with player instance
     */
    public HordePlayer(Player player, Arena arena) {
        this(player.getUniqueId(), arena);
    }

    /**
     * Saves the current player state for later restoration
     * Only called if arena config requires state saving
     */
    public void saveState() {
        Player player = getPlayer();
        if (player == null) {
            return;
        }
        
        this.savedState = new SavedPlayerState(player);
    }

    /**
     * Restores the player to their original state
     * Only called if state was previously saved
     */
    public void restoreState() {
        if (savedState == null) {
            return;
        }
        
        Player player = getPlayer();
        if (player == null) {
            return;
        }
        
        savedState.restore(player);
    }

    /**
     * Gets the Bukkit player instance
     * 
     * @return Player or null if offline
     */
    public Player getPlayer() {
        return arena.getPlugin().getServer().getPlayer(uuid);
    }

    /**
     * Increments kill count
     */
    public void addKill() {
        kills++;
    }

    /**
     * Increments death count and records death time
     */
    public void addDeath() {
        deaths++;
        deathTime = System.currentTimeMillis();
    }

    /**
     * Gets player name (safe for offline players)
     */
    public String getName() {
        Player player = getPlayer();
        return player != null ? player.getName() : "Unknown";
    }

    /**
     * Gets time spent in arena (in seconds)
     */
    public long getPlayTime() {
        return (System.currentTimeMillis() - joinTime) / 1000;
    }

    /**
     * Gets time since death (in seconds)
     */
    public long getTimeSinceDeath() {
        if (deathTime == 0) {
            return 0;
        }
        return (System.currentTimeMillis() - deathTime) / 1000;
    }

    /**
     * Checks if player is currently alive in the arena
     */
    public boolean isAlive() {
        return state == PlayerState.PLAYING;
    }

    /**
     * Checks if player is dead
     */
    public boolean isDead() {
        return state == PlayerState.DEAD || state == PlayerState.SPECTATING;
    }

    // Getters and Setters
    public UUID getUuid() {
        return uuid;
    }

    public Arena getArena() {
        return arena;
    }

    public PlayerState getState() {
        return state;
    }

    public void setState(PlayerState state) {
        this.state = state;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public boolean hasSavedState() {
        return savedState != null;
    }

    /**
     * Inner class to encapsulate saved player state
     * Reduces memory overhead by only storing when needed
     */
    private static class SavedPlayerState {
        
        // Inventory
        private final ItemStack[] inventory;
        private final ItemStack[] armor;
        private final ItemStack[] extraContents;
        private final ItemStack offHand;
        
        // Player state
        private final Location location;
        private final GameMode gameMode;
        private final double health;
        private final int foodLevel;
        private final float saturation;
        private final float exhaustion;
        private final float exp;
        private final int level;
        
        // Flight
        private final boolean allowFlight;
        private final boolean flying;
        
        // Effects
        private final Collection<PotionEffect> potionEffects;
        
        // Fire ticks
        private final int fireTicks;

        public SavedPlayerState(Player player) {
            // Clone inventory contents
            this.inventory = cloneArray(player.getInventory().getContents());
            this.armor = cloneArray(player.getInventory().getArmorContents());
            this.extraContents = cloneArray(player.getInventory().getExtraContents());
            this.offHand = cloneItem(player.getInventory().getItemInOffHand());
            
            // Save state
            this.location = player.getLocation().clone();
            this.gameMode = player.getGameMode();
            this.health = player.getHealth();
            this.foodLevel = player.getFoodLevel();
            this.saturation = player.getSaturation();
            this.exhaustion = player.getExhaustion();
            this.exp = player.getExp();
            this.level = player.getLevel();
            
            // Flight
            this.allowFlight = player.getAllowFlight();
            this.flying = player.isFlying();
            
            // Effects (create new collection to avoid concurrent modification)
            this.potionEffects = player.getActivePotionEffects();
            
            // Fire
            this.fireTicks = player.getFireTicks();
        }

        /**
         * Restores state to player
         */
        public void restore(Player player) {
            // Clear current state
            player.getInventory().clear();
            player.getActivePotionEffects().forEach(effect -> 
                player.removePotionEffect(effect.getType())
            );
            
            // Restore inventory
            player.getInventory().setContents(inventory);
            player.getInventory().setArmorContents(armor);
            player.getInventory().setExtraContents(extraContents);
            player.getInventory().setItemInOffHand(offHand);
            
            // Restore state
            player.teleport(location);
            player.setGameMode(gameMode);
            player.setHealth(Math.min(health, player.getMaxHealth()));
            player.setFoodLevel(foodLevel);
            player.setSaturation(saturation);
            player.setExhaustion(exhaustion);
            player.setExp(exp);
            player.setLevel(level);
            
            // Restore flight
            player.setAllowFlight(allowFlight);
            if (allowFlight) {
                player.setFlying(flying);
            }
            
            // Restore effects
            potionEffects.forEach(player::addPotionEffect);
            
            // Restore fire
            player.setFireTicks(fireTicks);
            
            // Update inventory
            player.updateInventory();
        }

        /**
         * Clones an item stack array
         */
        private ItemStack[] cloneArray(ItemStack[] array) {
            if (array == null) {
                return null;
            }
            
            ItemStack[] cloned = new ItemStack[array.length];
            for (int i = 0; i < array.length; i++) {
                cloned[i] = cloneItem(array[i]);
            }
            return cloned;
        }

        /**
         * Clones a single item stack
         */
        private ItemStack cloneItem(ItemStack item) {
            return item != null ? item.clone() : null;
        }
    }
}
