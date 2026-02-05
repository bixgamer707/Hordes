package me.bixgamer707.hordes.gui.player;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.arena.ArenaState;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for players to select and join arenas
 * 100% configurable from guis.yml
 */
public class ArenaSelectionGUI extends BaseGUI {

    private final List<Arena> arenas;
    private static final int ARENAS_PER_PAGE = 28; // 4 rows of 7 items
    
    // Arena display slots: rows 2-5, columns 2-8
    private static final int[] ARENA_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,  // Row 2
        19, 20, 21, 22, 23, 24, 25,  // Row 3
        28, 29, 30, 31, 32, 33, 34,  // Row 4
        37, 38, 39, 40, 41, 42, 43   // Row 5
    };

    public ArenaSelectionGUI(Hordes plugin, Player player) {
        super(plugin, player, "arena-selection");
        
        // Get all enabled arenas
        this.arenas = new ArrayList<>();
        for (Arena arena : plugin.getArenaManager().getArenas().values()) {
            if (arena.getConfig().isEnabled()) {
                arenas.add(arena);
            }
        }
        
        // Calculate max pages
        this.maxPages = (int) Math.ceil((double) arenas.size() / ARENAS_PER_PAGE);
        if (maxPages == 0) maxPages = 1;
    }

    @Override
    protected void buildDynamic() {
        // Static items (border, buttons, etc.) are already loaded by BaseGUI
        // We only add dynamic arena items here
        
        if (arenas.isEmpty()) {
            // Show "no arenas" message
            return;
        }
        
        // Calculate which arenas to show on this page
        int startIndex = currentPage * ARENAS_PER_PAGE;
        int endIndex = Math.min(startIndex + ARENAS_PER_PAGE, arenas.size());
        
        // Add arena items
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Arena arena = arenas.get(i);
            int slot = ARENA_SLOTS[slotIndex];
            
            // Create and place arena item
            ItemStack arenaItem = createArenaItem(arena);
            inventory.setItem(slot, arenaItem);
            
            // Register click handler
            final String arenaId = arena.getId();
            clickHandlers.put(slot + "", p -> handleArenaClick(p, arenaId));
            
            slotIndex++;
        }
        
        // Update dynamic placeholders in static items
        updateInfoItem();
        updatePaginationButtons();
    }

    /**
     * Creates an ItemStack for an arena based on its state
     */
    private ItemStack createArenaItem(Arena arena) {
        // Get material based on arena state (configured in guis.yml)
        Material material = getMaterialForState(arena.getState());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            return item;
        }
        
        // Get name from config and replace placeholders
        String nameTemplate = getConfigString("items.arena-item.name", "&e{arena_name}");
        String name = nameTemplate.replace("{arena_name}", arena.getConfig().getDisplayName());
        meta.setDisplayName(Text.createText(name).build(player));
        
        // Get lore from config and replace placeholders
        List<String> loreTemplate = guiConfig.getStringList("guis." + guiId + ".items.arena-item.lore");
        List<String> lore = new ArrayList<>();
        
        for (String line : loreTemplate) {
            String processed = line
                .replace("{arena_status}", getStatusText(arena))
                .replace("{current_players}", String.valueOf(arena.getPlayerCount()))
                .replace("{max_players}", String.valueOf(arena.getConfig().getMaxPlayers()))
                .replace("{total_waves}", String.valueOf(arena.getConfig().getTotalWaves()))
                .replace("{current_wave}", arena.getState() == ArenaState.ACTIVE ? 
                    String.valueOf(arena.getCurrentWave()) : "N/A");
            
            lore.add(Text.createText(processed).build(player));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }

    /**
     * Gets the material for an arena based on its state
     * Configured in guis.yml under arena-item.material-{state}
     */
    private Material getMaterialForState(ArenaState state) {
        String materialKey = "material-" + state.name().toLowerCase();
        String materialName = getConfigString("items.arena-item." + materialKey, "GRAY_WOOL");
        
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.logWarning("Invalid material in guis.yml: " + materialName);
            return Material.GRAY_WOOL;
        }
    }

    /**
     * Gets formatted status text for arena
     */
    private String getStatusText(Arena arena) {
        switch (arena.getState()) {
            case WAITING:
                return "&a&lWAITING";
            case STARTING:
                return "&e&lSTARTING";
            case ACTIVE:
                return "&6&lACTIVE";
            case ENDING:
                return "&c&lENDING";
            default:
                return "&7&lUNKNOWN";
        }
    }

    /**
     * Handles clicking on an arena item
     */
    private void handleArenaClick(Player player, String arenaId) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        
        if (arena == null) {
            player.sendMessage(Text.createText("&cArena not found!").build());
            close();
            return;
        }
        
        // Try to join the arena
        boolean success = plugin.getArenaManager().joinArena(player, arenaId);
        
        if (success) {
            // Successfully joined - close GUI
            close();
            playSound(getConfigString("sounds.open", "ENTITY_PLAYER_LEVELUP"));
        } else {
            // Failed to join - play error sound but keep GUI open
            playSound(getConfigString("sounds.error", "ENTITY_VILLAGER_NO"));
            // Refresh GUI in case arena state changed
            refresh();
        }
    }

    /**
     * Updates the info item with current arena count
     */
    private void updateInfoItem() {
        ItemStack infoItem = inventory.getItem(4);
        if (infoItem == null || !infoItem.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = infoItem.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }
        
        // Update lore with arena count
        List<String> lore = meta.getLore();
        List<String> newLore = new ArrayList<>();
        
        for (String line : lore) {
            newLore.add(line.replace("{arena_count}", String.valueOf(arenas.size())));
        }
        
        meta.setLore(newLore);
        infoItem.setItemMeta(meta);
        inventory.setItem(4, infoItem);
    }

    /**
     * Updates pagination buttons with current page info
     */
    private void updatePaginationButtons() {
        // Update or hide previous page button (slot 45)
        if (currentPage > 0) {
            updatePaginationButton(45, currentPage + 1, maxPages);
        } else {
            inventory.setItem(45, null); // Hide on first page
        }
        
        // Update or hide next page button (slot 53)
        if (currentPage < maxPages - 1) {
            updatePaginationButton(53, currentPage + 1, maxPages);
        } else {
            inventory.setItem(53, null); // Hide on last page
        }
    }

    /**
     * Updates a pagination button with page numbers
     */
    private void updatePaginationButton(int slot, int currentPageDisplay, int totalPages) {
        ItemStack button = inventory.getItem(slot);
        if (button == null || !button.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = button.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }
        
        // Update lore with page numbers
        List<String> lore = meta.getLore();
        List<String> newLore = new ArrayList<>();
        
        for (String line : lore) {
            newLore.add(line
                .replace("{current_page}", String.valueOf(currentPageDisplay))
                .replace("{total_pages}", String.valueOf(totalPages)));
        }
        
        meta.setLore(newLore);
        button.setItemMeta(meta);
        inventory.setItem(slot, button);
    }

    @Override
    protected void handleCustomAction(String actionType, String actionValue, String itemId) {
        // Handle custom actions
        switch (actionType) {
            case "join-arena":
                handleArenaClick(player, actionValue);
                break;
                
            case "arena-info":
                // Open arena info GUI
                Arena arena = plugin.getArenaManager().getArena(actionValue);
                if (arena != null) {
                    new ArenaInfoGUI(plugin, player, arena).open();
                }
                break;
        }
    }
}