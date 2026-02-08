package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.config.RewardType;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Material;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Reward editor GUI - 100% configurable from guis.yml
 * Allows editing all reward aspects: money, items, commands, type
 */
public class RewardEditorGUI extends BaseGUI {

    private final Arena arena;
    private final String arenaId;

    public RewardEditorGUI(Hordes plugin, Player player, Arena arena) {
        super(plugin, player, "admin-reward-editor");
        this.arena = arena;
        this.arenaId = arena.getId();
    }

    @Override
    protected void buildDynamic() {
        updateRewardStatus();
        updateRewardType();
        updateMoneyReward();
        updateItemRewards();
        updateCommandRewards();
        updateProgressiveMultiplier();
    }

    /**
     * Updates reward status (enabled/disabled)
     */
    private void updateRewardStatus() {
        int slot = getConfigInt("items.reward-status.slot", 4);
        boolean enabled = arena.getConfig().getRewardConfig().isEnabled();
        
        String materialKey = enabled ? "material-enabled" : "material-disabled";
        String materialName = getConfigString("items.reward-status." + materialKey, 
            enabled ? "EMERALD" : "REDSTONE");
        
        try {
            Material material = Material.valueOf(materialName);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                String name = getConfigString("items.reward-status.name", "&e&lReward System")
                    .replace("{status}", enabled ? 
                        getConfigString("text.enabled", "&a✔ Enabled") : 
                        getConfigString("text.disabled", "&c✖ Disabled"));
                meta.setDisplayName(Text.createText(name).build(player));
                
                List<String> lore = new ArrayList<>();
                for (String line : getConfigStringList("items.reward-status.lore")) {
                    lore.add(Text.createText(line
                        .replace("{status}", enabled ? "&aEnabled" : "&cDisabled"))
                        .build(player));
                }
                meta.setLore(lore);
                
                item.setItemMeta(meta);
            }
            
            inventory.setItem(slot, item);
            clickHandlers.put(slot + "", p -> toggleRewards());
            
        } catch (IllegalArgumentException e) {
            plugin.logWarning("Invalid material: " + materialName);
        }
    }

    /**
     * Updates reward type display
     */
    private void updateRewardType() {
        int slot = getConfigInt("items.reward-type.slot", 10);
        RewardType type = arena.getConfig().getRewardConfig().getType();
        
        String materialKey = "material-" + type.name().toLowerCase();
        String materialName = getConfigString("items.reward-type." + materialKey, "PAPER");
        
        try {
            Material material = Material.valueOf(materialName);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                String name = getConfigString("items.reward-type.name", "&6&lReward Type")
                    .replace("{type}", type.getDisplayName());
                meta.setDisplayName(Text.createText(name).build(player));
                
                List<String> lore = new ArrayList<>();
                for (String line : getConfigStringList("items.reward-type.lore")) {
                    lore.add(Text.createText(line
                        .replace("{type}", type.name())
                        .replace("{type_display}", type.getDisplayName()))
                        .build(player));
                }
                meta.setLore(lore);
                
                item.setItemMeta(meta);
            }
            
            inventory.setItem(slot, item);
            clickHandlers.put(slot + "", p -> cycleRewardType());
            
        } catch (IllegalArgumentException e) {
            plugin.logWarning("Invalid material: " + materialName);
        }
    }

    /**
     * Updates money reward display
     */
    private void updateMoneyReward() {
        int slot = getConfigInt("items.money-reward.slot", 12);
        double money = arena.getConfig().getRewardConfig().getMoney();
        
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = getConfigString("items.money-reward.name", "&6&lMoney Reward");
            meta.setDisplayName(Text.createText(name).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : getConfigStringList("items.money-reward.lore")) {
                lore.add(Text.createText(line
                    .replace("{money}", String.format("%.2f", money)))
                    .build(player));
            }
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editMoneyReward());
    }

    /**
     * Updates item rewards display
     */
    private void updateItemRewards() {
        int slot = getConfigInt("items.item-rewards.slot", 14);
        List<String> items = arena.getConfig().getRewardConfig().getItems();
        
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = getConfigString("items.item-rewards.name", "&b&lItem Rewards");
            meta.setDisplayName(Text.createText(name).build(player));
            
            List<String> lore = new ArrayList<>();
            List<String> loreTemplate = getConfigStringList("items.item-rewards.lore");
            
            for (String line : loreTemplate) {
                if (line.contains("{item_list}")) {
                    if (items.isEmpty()) {
                        lore.add(Text.createText("  &7No items configured").build(player));
                    } else {
                        int shown = Math.min(items.size(), 5);
                        for (int i = 0; i < shown; i++) {
                            lore.add(Text.createText("  &7- &e" + items.get(i)).build(player));
                        }
                        if (items.size() > 5) {
                            lore.add(Text.createText("  &7... and " + (items.size() - 5) + " more").build(player));
                        }
                    }
                } else {
                    lore.add(Text.createText(line
                        .replace("{item_count}", String.valueOf(items.size())))
                        .build(player));
                }
            }
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editItemRewards());
    }

    /**
     * Updates command rewards display
     */
    private void updateCommandRewards() {
        int slot = getConfigInt("items.command-rewards.slot", 16);
        List<String> commands = arena.getConfig().getRewardConfig().getCommands();
        
        ItemStack item = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = getConfigString("items.command-rewards.name", "&c&lCommand Rewards");
            meta.setDisplayName(Text.createText(name).build(player));
            
            List<String> lore = new ArrayList<>();
            List<String> loreTemplate = getConfigStringList("items.command-rewards.lore");
            
            for (String line : loreTemplate) {
                if (line.contains("{command_list}")) {
                    if (commands.isEmpty()) {
                        lore.add(Text.createText("  &7No commands configured").build(player));
                    } else {
                        int shown = Math.min(commands.size(), 3);
                        for (int i = 0; i < shown; i++) {
                            String cmd = commands.get(i);
                            String shortened = cmd.length() > 40 ? cmd.substring(0, 37) + "..." : cmd;
                            lore.add(Text.createText("  &7- &e" + shortened).build(player));
                        }
                        if (commands.size() > 3) {
                            lore.add(Text.createText("  &7... and " + (commands.size() - 3) + " more").build(player));
                        }
                    }
                } else {
                    lore.add(Text.createText(line
                        .replace("{command_count}", String.valueOf(commands.size())))
                        .build(player));
                }
            }
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editCommandRewards());
    }

    /**
     * Updates progressive multiplier display
     */
    private void updateProgressiveMultiplier() {
        int slot = getConfigInt("items.progressive-multiplier.slot", 22);
        double multiplier = arena.getConfig().getRewardConfig().getProgressiveMultiplier();
        
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = getConfigString("items.progressive-multiplier.name", "&a&lProgressive Multiplier");
            meta.setDisplayName(Text.createText(name).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : getConfigStringList("items.progressive-multiplier.lore")) {
                lore.add(Text.createText(line
                    .replace("{multiplier}", String.format("%.2f", multiplier))
                    .replace("{percentage}", String.format("%.0f", multiplier * 100)))
                    .build(player));
            }
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editProgressiveMultiplier());
    }

    /**
     * Toggle rewards enabled/disabled
     */
    private void toggleRewards() {
        boolean newState = !arena.getConfig().getRewardConfig().isEnabled();
        
        plugin.getFileManager().getArenas()
            .set("arenas." + arenaId + ".rewards.enabled", newState);
        plugin.getFileManager().getArenas().save();
        
        sendConfigMessage(newState ? "rewards.enabled" : "rewards.disabled");
        playSound(getConfigString("sounds.click", "UI_BUTTON_CLICK"));
        
        reloadAndRefresh();
    }

    /**
     * Cycle through reward types
     */
    private void cycleRewardType() {
        RewardType current = arena.getConfig().getRewardConfig().getType();
        RewardType[] types = RewardType.values();
        int nextIndex = (current.ordinal() + 1) % types.length;
        RewardType next = types[nextIndex];
        
        plugin.getFileManager().getArenas()
            .set("arenas." + arenaId + ".rewards.type", next.name());
        plugin.getFileManager().getArenas().save();
        
        sendConfigMessage("rewards.type-changed", next.getDisplayName());
        playSound(getConfigString("sounds.click", "UI_BUTTON_CLICK"));
        
        reloadAndRefresh();
    }

    /**
     * Edit money reward via conversation
     */
    private void editMoneyReward() {
        close();
        
        sendConfigMessage("rewards.edit-money-header");
        sendConfigMessage("rewards.edit-money-current", 
            String.format("%.2f", arena.getConfig().getRewardConfig().getMoney()));
        
        ConversationFactory factory = new ConversationFactory(plugin)
            .withFirstPrompt(new NumericPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return Text.createText(getConfigString("prompts.money-amount", 
                        "Enter money amount (0 to disable):")).build();
                }
                
                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                    double amount = input.doubleValue();
                    
                    if (amount < 0) {
                        sendConfigMessage("rewards.invalid-amount");
                        return this;
                    }
                    
                    plugin.getFileManager().getArenas()
                        .set("arenas." + arenaId + ".rewards.money", amount);
                    plugin.getFileManager().getArenas().save();
                    
                    sendConfigMessage("rewards.money-updated", String.format("%.2f", amount));
                    
                    return Prompt.END_OF_CONVERSATION;
                }
            })
            .withLocalEcho(false)
            .withTimeout(getConfigInt("prompts.timeout", 60))
            .addConversationAbandonedListener(event -> reopenAfterConversation())
            .buildConversation(player);
        
        factory.begin();
    }

    /**
     * Edit item rewards via conversation
     */
    private void editItemRewards() {
        close();
        
        sendConfigMessage("rewards.edit-items-header");
        sendConfigMessage("rewards.edit-items-help");
        
        ConversationFactory factory = new ConversationFactory(plugin)
            .withFirstPrompt(new StringPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return Text.createText(getConfigString("prompts.item-format", 
                        "Enter item (MATERIAL AMOUNT) or 'done':")).build();
                }
                
                @Override
                public Prompt acceptInput(ConversationContext context, String input) {
                    if (input.equalsIgnoreCase("done") || input.equalsIgnoreCase("cancel")) {
                        return Prompt.END_OF_CONVERSATION;
                    }
                    
                    // Validate format
                    String[] parts = input.split(" ");
                    if (parts.length < 1) {
                        sendConfigMessage("rewards.invalid-item-format");
                        return this;
                    }
                    
                    // Validate material
                    try {
                        Material.valueOf(parts[0].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        sendConfigMessage("rewards.invalid-material", parts[0]);
                        return this;
                    }
                    
                    // Validate amount if provided
                    if (parts.length >= 2) {
                        try {
                            int amount = Integer.parseInt(parts[1]);
                            if (amount <= 0 || amount > 64) {
                                sendConfigMessage("rewards.invalid-amount-range");
                                return this;
                            }
                        } catch (NumberFormatException e) {
                            sendConfigMessage("rewards.invalid-amount");
                            return this;
                        }
                    }
                    
                    // Add to list
                    List<String> items = plugin.getFileManager().getArenas()
                        .getStringList("arenas." + arenaId + ".rewards.items");
                    items.add(input);
                    
                    plugin.getFileManager().getArenas()
                        .set("arenas." + arenaId + ".rewards.items", items);
                    plugin.getFileManager().getArenas().save();
                    
                    sendConfigMessage("rewards.item-added", input);
                    
                    return this; // Continue conversation
                }
            })
            .withLocalEcho(false)
            .withTimeout(getConfigInt("prompts.timeout", 120))
            .addConversationAbandonedListener(event -> reopenAfterConversation())
            .buildConversation(player);
        
        factory.begin();
    }

    /**
     * Edit command rewards via conversation
     */
    private void editCommandRewards() {
        close();
        
        sendConfigMessage("rewards.edit-commands-header");
        sendConfigMessage("rewards.edit-commands-help");
        
        ConversationFactory factory = new ConversationFactory(plugin)
            .withFirstPrompt(new StringPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return Text.createText(getConfigString("prompts.command-format", 
                        "Enter command (without /) or 'done':")).build();
                }
                
                @Override
                public Prompt acceptInput(ConversationContext context, String input) {
                    if (input.equalsIgnoreCase("done") || input.equalsIgnoreCase("cancel")) {
                        return Prompt.END_OF_CONVERSATION;
                    }
                    
                    // Remove leading slash if present
                    if (input.startsWith("/")) {
                        input = input.substring(1);
                    }
                    
                    // Add to list
                    List<String> commands = plugin.getFileManager().getArenas()
                        .getStringList("arenas." + arenaId + ".rewards.commands");
                    commands.add(input);
                    
                    plugin.getFileManager().getArenas()
                        .set("arenas." + arenaId + ".rewards.commands", commands);
                    plugin.getFileManager().getArenas().save();
                    
                    sendConfigMessage("rewards.command-added", input);
                    
                    return this; // Continue conversation
                }
            })
            .withLocalEcho(false)
            .withTimeout(getConfigInt("prompts.timeout", 120))
            .addConversationAbandonedListener(event -> reopenAfterConversation())
            .buildConversation(player);
        
        factory.begin();
    }

    /**
     * Edit progressive multiplier via conversation
     */
    private void editProgressiveMultiplier() {
        close();
        
        sendConfigMessage("rewards.edit-multiplier-header");
        sendConfigMessage("rewards.edit-multiplier-current", 
            String.format("%.2f", arena.getConfig().getRewardConfig().getProgressiveMultiplier()));
        
        ConversationFactory factory = new ConversationFactory(plugin)
            .withFirstPrompt(new NumericPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return Text.createText(getConfigString("prompts.multiplier-amount", 
                        "Enter multiplier (0.0-1.0):")).build();
                }
                
                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                    double multiplier = input.doubleValue();
                    
                    if (multiplier < 0 || multiplier > 1.0) {
                        sendConfigMessage("rewards.invalid-multiplier-range");
                        return this;
                    }
                    
                    plugin.getFileManager().getArenas()
                        .set("arenas." + arenaId + ".rewards.progressive-multiplier", multiplier);
                    plugin.getFileManager().getArenas().save();
                    
                    sendConfigMessage("rewards.multiplier-updated", String.format("%.2f", multiplier));
                    
                    return Prompt.END_OF_CONVERSATION;
                }
            })
            .withLocalEcho(false)
            .withTimeout(getConfigInt("prompts.timeout", 60))
            .addConversationAbandonedListener(event -> reopenAfterConversation())
            .buildConversation(player);
        
        factory.begin();
    }

    /**
     * Reload arena and refresh GUI
     */
    private void reloadAndRefresh() {
        plugin.getArenaManager().reloadArenas();
        Arena reloaded = plugin.getArenaManager().getArena(arenaId);
        if (reloaded != null) {
            new RewardEditorGUI(plugin, player, reloaded).open();
        }
    }

    /**
     * Reopen GUI after conversation
     */
    private void reopenAfterConversation() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getArenaManager().reloadArenas();
            Arena reloaded = plugin.getArenaManager().getArena(arenaId);
            if (reloaded != null) {
                new RewardEditorGUI(plugin, player, reloaded).open();
            }
        }, 1L);
    }

    @Override
    protected void handleCustomAction(String actionType, String actionValue, String itemId) {
        switch (actionType) {
            case "clear-items":
                plugin.getFileManager().getArenas()
                    .set("arenas." + arenaId + ".rewards.items", new ArrayList<>());
                plugin.getFileManager().getArenas().save();
                sendConfigMessage("rewards.items-cleared");
                playSound("success");
                reloadAndRefresh();
                break;
                
            case "clear-commands":
                plugin.getFileManager().getArenas()
                    .set("arenas." + arenaId + ".rewards.commands", new ArrayList<>());
                plugin.getFileManager().getArenas().save();
                sendConfigMessage("rewards.commands-cleared");
                playSound("success");
                reloadAndRefresh();
                break;
        }
    }

    @Override
    protected void onBack() {
        new ArenaEditorGUI(plugin, player, arena).open();
    }
}