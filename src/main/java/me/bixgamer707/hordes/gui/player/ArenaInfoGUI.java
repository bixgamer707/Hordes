package me.bixgamer707.hordes.gui.player;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.arena.ArenaState;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.player.HordePlayer;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GUI showing detailed information about a specific arena
 * 100% configurable from guis.yml
 */
public class ArenaInfoGUI extends BaseGUI {

    private final Arena arena;

    public ArenaInfoGUI(Hordes plugin, Player player, Arena arena) {
        super(plugin, player, "arena-info");
        this.arena = arena;
    }

    @Override
    protected void buildDynamic() {
        // Static items are already loaded by BaseGUI from guis.yml
        // We update them with arena-specific data
        
        updateStatusItem();
        updateGeneralInfo();
        updateWaveInfo();
        updateRewards();
        updateDifficulty();
        updatePlayersList();
        updateJoinButton();
    }

    /**
     * Updates the arena status item (slot 4)
     */
    private void updateStatusItem() {
        ItemStack item = inventory.getItem(4);
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        // Update lore with arena data
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            List<String> newLore = new ArrayList<>();
            
            for (String line : lore) {
                String processed = line
                    .replace("{arena_name}", arena.getConfig().getDisplayName())
                    .replace("{arena_state}", getStateText(arena.getState()))
                    .replace("{current_players}", String.valueOf(arena.getPlayerCount()))
                    .replace("{max_players}", String.valueOf(arena.getConfig().getMaxPlayers()))
                    .replace("{joinable_status}", getJoinableStatus());
                
                newLore.add(Text.createText(processed).build(player));
            }
            
            meta.setLore(newLore);
            item.setItemMeta(meta);
            inventory.setItem(4, item);
        }
    }

    /**
     * Updates general information item (slot 10)
     */
    private void updateGeneralInfo() {
        ItemStack item = inventory.getItem(10);
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }
        
        List<String> lore = meta.getLore();
        List<String> newLore = new ArrayList<>();
        
        for (String line : lore) {
            String processed = line
                .replace("{min_players}", String.valueOf(arena.getConfig().getMinPlayers()))
                .replace("{max_players}", String.valueOf(arena.getConfig().getMaxPlayers()))
                .replace("{mode}", arena.getConfig().getSurvivalMode().isEnabled() ? "Survival" : "Arena")
                .replace("{progression}", arena.getConfig().getProgressionType().name());
            
            newLore.add(Text.createText(processed).build(player));
        }
        
        meta.setLore(newLore);
        item.setItemMeta(meta);
        inventory.setItem(10, item);
    }

    /**
     * Updates wave information item (slot 12)
     */
    private void updateWaveInfo() {
        ItemStack item = inventory.getItem(12);
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }
        
        List<String> lore = meta.getLore();
        List<String> newLore = new ArrayList<>();
        
        for (String line : lore) {
            String processed = line
                .replace("{total_waves}", String.valueOf(arena.getConfig().getTotalWaves()))
                .replace("{wave_delay}", String.valueOf(arena.getConfig().getWaveDelay()))
                .replace("{current_wave}", arena.getState() == ArenaState.ACTIVE ? 
                    String.valueOf(arena.getCurrentWave()) : "Not started");
            
            newLore.add(Text.createText(processed).build(player));
        }
        
        meta.setLore(newLore);
        item.setItemMeta(meta);
        inventory.setItem(12, item);
    }

    /**
     * Updates rewards item (slot 14)
     */
    private void updateRewards() {
        ItemStack item = inventory.getItem(14);
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }
        
        List<String> lore = meta.getLore();
        List<String> newLore = new ArrayList<>();
        
        // Get reward info
        String rewardType = arena.getConfig().getRewardConfig().getType().name();
        double moneyReward = arena.getConfig().getRewardConfig().getMoney();
        int itemCount = arena.getConfig().getRewardConfig().getItems().size();
        
        for (String line : lore) {
            String processed = line
                .replace("{reward_type}", rewardType)
                .replace("{money_reward}", String.format("%.2f", moneyReward))
                .replace("{item_count}", String.valueOf(itemCount));
            
            newLore.add(Text.createText(processed).build(player));
        }
        
        meta.setLore(newLore);
        item.setItemMeta(meta);
        inventory.setItem(14, item);
    }

    /**
     * Updates difficulty indicator (slot 16)
     */
    private void updateDifficulty() {
        ItemStack item = inventory.getItem(16);
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }
        
        // Calculate difficulty based on waves
        int waves = arena.getConfig().getTotalWaves();
        String difficulty;
        if (waves <= 5) {
            difficulty = "&a&lEASY";
        } else if (waves <= 10) {
            difficulty = "&e&lMEDIUM";
        } else {
            difficulty = "&c&lHARD";
        }
        
        // Estimate time
        int estimatedMinutes = (waves * (arena.getConfig().getWaveDelay() + 30)) / 60;
        
        List<String> lore = meta.getLore();
        List<String> newLore = new ArrayList<>();
        
        for (String line : lore) {
            String processed = line
                .replace("{difficulty}", difficulty)
                .replace("{estimated_time}", String.valueOf(estimatedMinutes));
            
            newLore.add(Text.createText(processed).build(player));
        }
        
        meta.setLore(newLore);
        item.setItemMeta(meta);
        inventory.setItem(16, item);
    }

    /**
     * Updates players list (slot 22)
     */
    private void updatePlayersList() {
        ItemStack item = inventory.getItem(22);
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }
        
        List<String> lore = meta.getLore();
        List<String> newLore = new ArrayList<>();
        
        // Build player list
        StringBuilder playerList = new StringBuilder();
        Collection<HordePlayer> playersData = arena.getPlayers().values();

        List<HordePlayer> players = new ArrayList<>(playersData);
        
        if (players.isEmpty()) {
            playerList.append("&7No players");
        } else {
            int shown = Math.min(players.size(), 10);
            for (int i = 0; i < shown; i++) {
                HordePlayer hp = players.get(i);
                playerList.append("&7- &e").append(hp.getPlayer().getName());
                if (i < shown - 1) {
                    playerList.append("\n");
                }
            }
            
            if (players.size() > 10) {
                playerList.append("\n&7... and ").append(players.size() - 10).append(" more");
            }
        }
        
        for (String line : lore) {
            String processed = line
                .replace("{current_players}", String.valueOf(arena.getPlayerCount()))
                .replace("{player_list}", playerList.toString());
            
            newLore.add(Text.createText(processed).build(player));
        }
        
        meta.setLore(newLore);
        item.setItemMeta(meta);
        inventory.setItem(22, item);
    }

    /**
     * Updates join button visibility (slot 32)
     */
    private void updateJoinButton() {
        // Only show join button if arena is joinable
        if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.STARTING) {
            inventory.setItem(32, null); // Hide button
        }
        // Button is already created by BaseGUI if present
    }

    /**
     * Gets colored state text
     */
    private String getStateText(ArenaState state) {
        switch (state) {
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
     * Gets joinable status text
     */
    private String getJoinableStatus() {
        if (arena.getState() == ArenaState.WAITING || arena.getState() == ArenaState.STARTING) {
            return "&a✔ Joinable";
        } else {
            return "&c✘ Not joinable";
        }
    }

    @Override
    protected void handleCustomAction(int slot, String actionType, String actionValue, String itemId) {
        switch (actionType) {
            case "join-arena":
                // Try to join the arena
                boolean success = plugin.getArenaManager().joinArena(player, arena.getId());
                if (success) {
                    close();
                    playSound(guiConfig.getString("guis."+guiId+".sounds.success", "ENTITY_PLAYER_LEVELUP"));
                } else {
                    playSound(guiConfig.getString("guis."+guiId+".sounds.error", "ENTITY_VILLAGER_NO"));
                }
                break;
                
            case "refresh":
                // Refresh the GUI
                refresh();
                playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
                break;
        }
    }

    @Override
    protected void onBack() {
        // Return to arena selection
        new ArenaSelectionGUI(plugin, player).open();
    }
}