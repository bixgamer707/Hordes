package me.bixgamer707.hordes.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles all GUI-related events
 * Prevents item movement, handles clicks, cleanup on close
 */
public class GUIListener implements Listener {

    /**
     * Handles inventory clicks in GUIs
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        BaseGUI gui = BaseGUI.getOpenGUI(player);
        
        if (gui == null) {
            return;
        }
        
        // Check if clicked in GUI inventory
        if (!event.getInventory().equals(gui.getInventory())) {
            return;
        }
        
        // Cancel all clicks in GUIs
        event.setCancelled(true);
        
        // Handle the click
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < gui.getInventory().getSize()) {
            gui.handleClick(slot, event.getClick());
        }
    }

    /**
     * Handles inventory close
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        BaseGUI gui = BaseGUI.getOpenGUI(player);
        
        if (gui != null && event.getInventory().equals(gui.getInventory())) {
            gui.onClose();
        }
    }

    /**
     * Cleanup on player quit
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BaseGUI.closeGUI(event.getPlayer());
    }
}
