package me.bixgamer707.hordes.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.arena.ArenaManager;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles WorldGuard region events
 * Enables auto-join when entering arena regions
 * 
 * Note: This uses PlayerMoveEvent for region detection
 * WorldGuard 7.0+ doesn't have RegionEnterEvent anymore
 */
public class WorldGuardListener implements Listener {

    private final Hordes plugin;
    private final ArenaManager arenaManager;
    
    // Track which region each player is in
    private final Map<UUID, String> playerRegions;
    
    // WorldGuard integration status
    private boolean worldGuardEnabled;

    public WorldGuardListener(Hordes plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
        this.playerRegions = new HashMap<>();
        
        checkWorldGuard();
    }

    /**
     * Checks if WorldGuard is available
     */
    private void checkWorldGuard() {
        worldGuardEnabled = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        
        if (worldGuardEnabled) {
            plugin.logInfo("WorldGuard integration enabled");
        } else {
            plugin.logInfo("WorldGuard not found - region features disabled");
        }
    }

    /**
     * Handles player movement for region detection
     * This is more performance-intensive than RegionEnterEvent
     * but it's the only option in WorldGuard 7.0+
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!worldGuardEnabled) {
            return;
        }
        
        // Only check if player moved to a different block
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) {
            return;
        }
        
        // Optimization: only check if changed block
        if (from.getBlockX() == to.getBlockX() && 
            from.getBlockY() == to.getBlockY() && 
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Get regions at new location
        Set<String> newRegions = getRegionsAtLocation(to);
        String currentRegion = playerRegions.get(uuid);
        
        // Check for region enter
        for (String regionName : newRegions) {
            if (currentRegion == null || !currentRegion.equals(regionName)) {
                handleRegionEnter(player, regionName);
                playerRegions.put(uuid, regionName);
                return;
            }
        }
        
        // Check for region leave
        if (currentRegion != null && !newRegions.contains(currentRegion)) {
            handleRegionLeave(player, currentRegion);
            playerRegions.remove(uuid);
        }
    }

    /**
     * Gets regions at a location
     */
    private Set<String> getRegionsAtLocation(Location location) {
        try {
            // WorldGuard 7.0+ API
            com.sk89q.worldedit.util.Location wgLoc = BukkitAdapter.adapt(location);
            
            WorldGuard wg = WorldGuard.getInstance();
            RegionManager regionManager =
                wg.getPlatform().getRegionContainer().get(
                   BukkitAdapter.adapt(location.getWorld())
                );
            
            if (regionManager == null) {
                return Set.of();
            }
            
            ApplicableRegionSet regions = regionManager.getApplicableRegions(
                com.sk89q.worldedit.math.BlockVector3.at(
                    location.getBlockX(), 
                    location.getBlockY(), 
                    location.getBlockZ()
                )
            );
            
            return regions.getRegions().stream()
                .map(ProtectedRegion::getId)
                .collect(java.util.stream.Collectors.toSet());
                
        } catch (Exception e) {
            plugin.logWarning("Error getting regions at location: " + e.getMessage());
            return Set.of();
        }
    }

    /**
     * Handles entering a region
     */
    private void handleRegionEnter(Player player, String regionName) {
        // Check if this region is an arena
        Arena arena = arenaManager.getArenaByRegion(regionName);
        
        if (arena == null) {
            return;
        }
        
        // Check if player is already in an arena
        if (arenaManager.isInArena(player)) {
            return;
        }
        
        // Attempt to join
        boolean success = arenaManager.joinArena(player, arena.getId());
        
        if (!success) {
            // Join failed - send message why (already sent by Arena.canJoin)
        }
    }

    /**
     * Handles leaving a region
     */
    private void handleRegionLeave(Player player, String regionName) {
        // Check if this region is an arena
        Arena arena = arenaManager.getArenaByRegion(regionName);
        
        if (arena == null) {
            return;
        }
        
        // Check if player is in this arena
        Arena playerArena = arenaManager.getPlayerArena(player);
        
        if (playerArena != arena) {
            return;
        }
        
        // Only auto-leave if in lobby state
        if (playerArena.getState().isJoinable()) {
            arenaManager.leaveArena(player, true);
        } else {
            // In active arena - prevent leaving
            Text.createTextWithLang("arena.cannot-leave-area", player);
            
            // Teleport back to arena
            player.teleport(arena.getConfig().getExitLocation());
        }
    }

    /**
     * Cleanup on player quit
     */
    public void cleanupPlayer(UUID uuid) {
        playerRegions.remove(uuid);
    }

    /**
     * Reload WorldGuard integration
     */
    public void reload() {
        checkWorldGuard();
    }

    /**
     * Check if WorldGuard is enabled
     */
    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
}