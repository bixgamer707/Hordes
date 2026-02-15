package me.bixgamer707.hordes.listeners;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.arena.ArenaManager;
import me.bixgamer707.hordes.arena.ArenaState;
import me.bixgamer707.hordes.config.ArenaConfig;
import me.bixgamer707.hordes.config.ItemDropMode;
import me.bixgamer707.hordes.player.HordePlayer;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Handles all player-related events
 * - Death and respawn
 * - Quit and join
 * - Interactions and commands
 * - Item pickup and drop
 */
public class PlayerListener implements Listener {

    private final Hordes plugin;
    private final ArenaManager arenaManager;
    
    // Track dropped items for drop mode enforcement
    private final Map<UUID, UUID> itemOwners; // ItemUUID -> PlayerUUID

    public PlayerListener(Hordes plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
        this.itemOwners = new HashMap<>();
    }

    /**
     * Handles player death in arenas
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Arena arena = arenaManager.getPlayerArena(player);
        
        if (arena == null) {
            return;
        }
        
        HordePlayer hordePlayer = arena.getHordePlayer(player.getUniqueId());
        
        if (hordePlayer == null) {
            return;
        }
        
        ArenaConfig.ItemHandlingConfig itemConfig = arena.getConfig().getItemHandling();
        
        // Handle keep inventory
        if (itemConfig.shouldKeepInventory()) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        } else if (itemConfig.shouldDropItems()) {
            // Handle item drops based on mode
            handleItemDrops(event, arena, player);
        }
        
        // Keep level if configured
        event.setKeepLevel(itemConfig.shouldKeepInventory());
        
        // Notify arena of death
        arena.onPlayerDeath(hordePlayer);
    }

    /**
     * Handles item drops based on drop mode
     */
    private void handleItemDrops(PlayerDeathEvent event, Arena arena, Player player) {
        ItemDropMode dropMode = arena.getConfig().getItemHandling().getDropMode();
        
        if (dropMode == ItemDropMode.TELEPORT_WITH_PLAYER) {
            // Store items to give back later
            storeItemsForPlayer(player, event.getDrops());
            event.getDrops().clear();
            event.setDroppedExp(0);
            
        } else {
            // Mark dropped items with metadata
            List<ItemStack> drops = new ArrayList<>(event.getDrops());
            event.getDrops().clear();
            
            // Schedule item drop (after respawn to get proper location)
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (ItemStack item : drops) {
                        if (item != null && item.getType().isItem()) {
                            Item droppedItem = player.getWorld().dropItemNaturally(
                                player.getLocation(), 
                                item
                            );
                            
                            // Mark item owner
                            droppedItem.setMetadata("hordes_owner", 
                                new FixedMetadataValue(plugin, player.getUniqueId().toString()));
                            droppedItem.setMetadata("hordes_arena", 
                                new FixedMetadataValue(plugin, arena.getId()));
                            
                            itemOwners.put(droppedItem.getUniqueId(), player.getUniqueId());
                        }
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    /**
     * Stores items for teleporting with player
     */
    private void storeItemsForPlayer(Player player, List<ItemStack> items) {
        // Store in player metadata for retrieval after teleport
        player.setMetadata("hordes_stored_items", 
            new FixedMetadataValue(plugin, new ArrayList<>(items)));
    }

    /**
     * Handles player respawn
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Arena arena = arenaManager.getPlayerArena(player);
        
        if (arena == null) {
            return;
        }
        
        HordePlayer hordePlayer = arena.getHordePlayer(player.getUniqueId());
        
        if (hordePlayer == null) {
            return;
        }
        
        // Respawn location is handled by Arena.onPlayerDeath()
        // The death handling methods will teleport the player appropriately
        
        // Restore stored items if using TELEPORT_WITH_PLAYER mode
        if (player.hasMetadata("hordes_stored_items")) {
            List<?> storedItems = (List<?>) player.getMetadata("hordes_stored_items").get(0).value();
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (storedItems != null) {
                        for (Object obj : storedItems) {
                            if (obj instanceof ItemStack) {
                                player.getInventory().addItem((ItemStack) obj);
                            }
                        }
                    }
                    player.removeMetadata("hordes_stored_items", plugin);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    /**
     * Handles player quit
     * Removes player from arena gracefully
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Arena arena = arenaManager.getPlayerArena(player);
        
        if (arena == null) {
            return;
        }
        
        // Remove player from arena (restore state)
        arenaManager.leaveArena(player, true);
    }

    /**
     * Handles player join
     * Allows rejoining if configured
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if player was in an arena before disconnect
        // This is for future implementation of rejoin on disconnect
        // For now, players are removed on quit
    }

    /**
     * Handles player interact
     * Manual wave progression via buttons/signs
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Arena arena = arenaManager.getPlayerArena(player);
        
        if (arena == null || !arena.isWaitingForManualProgression()) {
            return;
        }
        
        // Check if clicking a button or sign
        if (event.getClickedBlock() == null) {
            return;
        }
        
        String blockType = event.getClickedBlock().getType().name();
        
        if (blockType.contains("BUTTON") || blockType.contains("SIGN")) {
            // Check if it's marked as a progression trigger
            if (event.getClickedBlock().hasMetadata("hordes_progression")) {
                String triggerArenaId = event.getClickedBlock()
                    .getMetadata("hordes_progression").get(0).asString();
                
                if (triggerArenaId.equals(arena.getId())) {
                    arena.triggerNextWave();
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Handles item pickup
     * Enforces drop mode restrictions
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        Item item = event.getItem();
        
        // Check if item has owner metadata
        if (!item.hasMetadata("hordes_owner")) {
            return;
        }
        
        String ownerUuidStr = item.getMetadata("hordes_owner").get(0).asString();
        UUID ownerUuid = UUID.fromString(ownerUuidStr);
        
        String arenaId = item.getMetadata("hordes_arena").get(0).asString();
        Arena arena = arenaManager.getArena(arenaId);
        
        if (arena == null) {
            return;
        }
        
        ItemDropMode dropMode = arena.getConfig().getItemHandling().getDropMode();
        
        switch (dropMode) {
            case OWNER_ONLY:
                // Only owner can pick up
                if (!player.getUniqueId().equals(ownerUuid)) {
                    event.setCancelled(true);
                }
                break;
                
            case ARENA_PLAYERS:
                // Only arena players can pick up
                if (!arena.hasPlayer(player.getUniqueId())) {
                    event.setCancelled(true);
                }
                break;
                
            case ALL_PLAYERS:
                // Anyone can pick up
                break;
                
            case TELEPORT_WITH_PLAYER:
                // Items shouldn't be on ground
                break;
        }
        
        // Clean up tracking if picked up
        if (!event.isCancelled()) {
            itemOwners.remove(item.getUniqueId());
        }
    }

    /**
     * Handles item drop
     * Prevents dropping items if configured
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Arena arena = arenaManager.getPlayerArena(player);
        
        if (arena == null) {
            return;
        }
        
        // Check if item drops are allowed
        ArenaConfig.ItemHandlingConfig itemConfig = arena.getConfig().getItemHandling();
        
        if (!itemConfig.shouldDropItems()) {
            event.setCancelled(true);
            Text.createTextWithLang("arena.cannot-drop-items", player);
        }
    }

    /**
     * Handles command preprocessing
     * Blocks commands in arena if configured
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Arena arena = arenaManager.getPlayerArena(player);

        if (arena == null) {
            return;
        }

        String command = event.getMessage().toLowerCase();

        // Allow /hordes leave always
        if (command.startsWith("/hordes leave") || command.startsWith("/hd leave")) {
            event.setCancelled(false);
            return;
        }

        List<String> commands = plugin.getFileManager().getConfig().getStringList("commands.blocked-commands");

        for (String blocked : commands) {
            if (command.startsWith(blocked.toLowerCase())) {
                event.setCancelled(true);
                Text.createTextWithLang("arena.cannot-use-command", player);
                break;
            }
        }
    }
    /**
     * Handles player teleport
     * Prevents teleporting out of arena
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Arena arena = arenaManager.getPlayerArena(player);
        
        if (arena == null) {
            return;
        }
        
        // Allow teleports from plugin
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }
        
        // Block other teleports (enderpearl, chorus fruit, etc.)
        if (arena.getState() == ArenaState.ACTIVE) {
            event.setCancelled(true);
            Text.createTextWithLang("arena.cannot-teleport", player);
        }
    }

    /**
     * Cleanup expired item tracking
     */
    public void cleanupItemTracking() {
        itemOwners.entrySet().removeIf(entry -> {
            UUID itemUuid = entry.getKey();
            // Item no longer exists
            return Bukkit.getEntity(itemUuid) == null;
        });
    }

    public void onAsyncJoin(AsyncPlayerPreLoginEvent event){
        UUID uuid = event.getUniqueId();


    }
}