package me.bixgamer707.hordes.rewards;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.config.ArenaConfig;
import me.bixgamer707.hordes.text.Text;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Manages reward distribution to players
 * Supports money, items, and command execution
 * 
 * Integrates with Vault for economy
 */
public class RewardManager {

    private final Hordes plugin;
    private Economy economy;
    private boolean economyEnabled;

    public RewardManager(Hordes plugin) {
        this.plugin = plugin;
        this.economyEnabled = false;
        
        setupEconomy();
    }

    /**
     * Sets up Vault economy integration
     */
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            Bukkit.getLogger().info("[Hordes] Vault not found - economy rewards disabled");
            return;
        }
        
        try {
            RegisteredServiceProvider<Economy> rsp = 
                Bukkit.getServicesManager().getRegistration(Economy.class);
            
            if (rsp == null) {
                Bukkit.getLogger().warning("[Hordes] No economy plugin found - economy rewards disabled");
                return;
            }
            
            economy = rsp.getProvider();
            economyEnabled = true;
            Bukkit.getLogger().info("[Hordes] Economy integration enabled via Vault");
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Hordes] Failed to setup economy: " + e.getMessage());
            economyEnabled = false;
        }
    }

    /**
     * Gives completion rewards to a player (full arena clear)
     * 
     * @param player Player to reward
     * @param config Reward configuration
     */
    public void giveCompletionReward(Player player, ArenaConfig.RewardConfig config) {
        if (!config.isEnabled()) {
            return;
        }
        
        // Give money
        if (config.getMoney() > 0) {
            giveMoney(player, config.getMoney());
        }
        
        // Give items
        giveItems(player, config.getItems());
        
        // Execute commands
        executeCommands(player, config.getCommands());
        
        // Send reward message
        Text.createTextWithLang("rewards.completion", player);
    }

    /**
     * Gives progressive rewards to a player (per wave)
     * 
     * @param player Player to reward
     * @param config Reward configuration
     * @param multiplier Reward multiplier (e.g., 0.1 for 10% of full reward)
     */
    public void giveProgressiveReward(Player player, ArenaConfig.RewardConfig config, double multiplier) {
        if (!config.isEnabled() || multiplier <= 0) {
            return;
        }
        
        // Give scaled money
        if (config.getMoney() > 0) {
            double scaledMoney = config.getMoney() * multiplier;
            giveMoney(player, scaledMoney);
        }
        
        // Items are not scaled (would be fractional)
        // Could implement a chance-based system instead
        
        // Commands are not executed for progressive rewards
        
        // Send reward message
        Text.createTextWithLang("rewards.progressive", player);
    }

    /**
     * Gives money to a player
     * 
     * @param player Player to give money to
     * @param amount Amount of money
     */
    private void giveMoney(Player player, double amount) {
        if (!economyEnabled || economy == null) {
            return;
        }
        
        try {
            economy.depositPlayer(player, amount);
            
            String formatted = economy.format(amount);
            player.sendMessage(Text.createTextWithLang("rewards.money")
                .replace("%0%", formatted)
                .build(player));
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Hordes] Failed to give money to " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Gives items to a player
     * 
     * @param player Player to give items to
     * @param itemStrings List of item strings (format: "MATERIAL AMOUNT" or "MATERIAL AMOUNT DATA")
     */
    private void giveItems(Player player, List<String> itemStrings) {
        if (itemStrings == null || itemStrings.isEmpty()) {
            return;
        }
        
        List<ItemStack> items = parseItems(itemStrings);
        
        for (ItemStack item : items) {
            // Try to add to inventory
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            
            // Drop items that don't fit
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            }
        }
        
        if (!items.isEmpty()) {
            player.sendMessage(Text.createTextWithLang("rewards.items").replace(
                "%0%", String.valueOf(items.size()
            )).build(player));
        }
    }

    /**
     * Parses item strings into ItemStacks
     * 
     * @param itemStrings List of item strings
     * @return List of ItemStacks
     */
    private List<ItemStack> parseItems(List<String> itemStrings) {
        List<ItemStack> items = new ArrayList<>();
        
        for (String itemStr : itemStrings) {
            try {
                ItemStack item = parseItem(itemStr);
                if (item != null) {
                    items.add(item);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Hordes] Failed to parse item: " + itemStr + " - " + e.getMessage());
            }
        }
        
        return items;
    }

    /**
     * Parses a single item string
     * Format: "MATERIAL AMOUNT" or "MATERIAL:DATA AMOUNT"
     * 
     * @param itemStr Item string
     * @return ItemStack or null if invalid
     */
    private ItemStack parseItem(String itemStr) {
        String[] parts = itemStr.split(" ");
        
        if (parts.length < 1) {
            return null;
        }
        
        // Parse material
        String materialStr = parts[0];
        Material material;
        
        try {
            material = Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[Hordes] Invalid material: " + materialStr);
            return null;
        }
        
        // Parse amount
        int amount = 1;
        if (parts.length >= 2) {
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("[Hordes] Invalid amount: " + parts[1]);
            }
        }
        
        return new ItemStack(material, amount);
    }

    /**
     * Executes commands for a player
     * Supports %player% placeholder
     * 
     * @param player Player
     * @param commands List of commands
     */
    private void executeCommands(Player player, List<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        
        for (String command : commands) {
            String processedCommand = command.replace("%player%", player.getName());
            
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Hordes] Failed to execute command: " + processedCommand + " - " + e.getMessage());
            }
        }
    }

    /**
     * Checks if economy is enabled
     * 
     * @return true if economy is available
     */
    public boolean isEconomyEnabled() {
        return economyEnabled;
    }

    /**
     * Reloads economy integration
     */
    public void reload() {
        setupEconomy();
    }
}