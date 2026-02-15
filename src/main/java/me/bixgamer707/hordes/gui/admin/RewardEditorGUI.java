package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.config.RewardType;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import me.bixgamer707.hordes.utils.InputValidators;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Reward editor GUI - 100% configurable
 * Edit all reward settings using ChatInputManager
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

    private void updateRewardStatus() {
        int slot = guiConfig.getInt("guis."+guiId+".items.reward-status.slot", 4);
        boolean enabled = arena.getConfig().getRewardConfig().isEnabled();

        String materialKey = enabled ? "material-enabled" : "material-disabled";
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.reward-status." + materialKey, enabled ? "EMERALD" : "REDSTONE")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.reward-status.name", "&6&lReward System")
                    .replace("{status}", enabled ? "&aEnabled" : "&cDisabled");
            meta.setDisplayName(Text.createText(name).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.reward-status.lore")) {
                lore.add(Text.createText(line.replace("{status}", enabled ? "&aEnabled" : "&cDisabled"))
                        .build(player));
            }
            meta.setLore(lore);

            if (enabled) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> toggleRewardStatus());
    }

    private void updateRewardType() {
        int slot = guiConfig.getInt("guis."+guiId+".items.reward-type.slot", 10);
        RewardType type = arena.getConfig().getRewardConfig().getType();

        String materialKey = "material-" + type.name().toLowerCase();
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.reward-type." + materialKey, "GOLD_INGOT")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.reward-type.name", "&e&lReward Type")).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.reward-type.lore")) {
                String processed = line
                        .replace("{type}", type.getDisplayName())
                        .replace("{type_desc}", getRewardTypeDescription(type));
                lore.add(Text.createText(processed).build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> cycleRewardType());
    }

    private void updateMoneyReward() {
        int slot = guiConfig.getInt("guis."+guiId+".items.money-reward.slot", 12);
        double money = arena.getConfig().getRewardConfig().getMoney();

        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.money-reward.material", "GOLD_INGOT")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.money-reward.name", "&6&lMoney Reward")).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.money-reward.lore")) {
                lore.add(Text.createText(line.replace("{money}", String.format("%.2f", money)))
                        .build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editMoneyReward());
    }

    private void updateItemRewards() {
        int slot = guiConfig.getInt("guis."+guiId+".items.item-rewards.slot", 14);
        List<String> items = arena.getConfig().getRewardConfig().getItems();

        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.item-rewards.material", "DIAMOND")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.item-rewards.name", "&b&lItem Rewards")).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.item-rewards.lore-header")) {
                lore.add(Text.createText(line.replace("{count}", String.valueOf(items.size())))
                        .build(player));
            }

            if (!items.isEmpty()) {
                lore.add("");
                int shown = Math.min(items.size(), 5);
                for (int i = 0; i < shown; i++) {
                    lore.add(Text.createText("&7- &e" + items.get(i)).build(player));
                }
                if (items.size() > 5) {
                    lore.add(Text.createText("&7... and " + (items.size() - 5) + " more").build(player));
                }
            }

            lore.add("");
            for (String line : guiConfig.getStringList("guis."+guiId+".items.item-rewards.lore-footer")) {
                lore.add(Text.createText(line).build(player));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editItemRewards());
    }

    private void updateCommandRewards() {
        int slot = guiConfig.getInt("guis."+guiId+".items.command-rewards.slot", 16);
        List<String> commands = arena.getConfig().getRewardConfig().getCommands();

        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.command-rewards.material", "COMMAND_BLOCK")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.command-rewards.name", "&c&lCommand Rewards")).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.command-rewards.lore-header")) {
                lore.add(Text.createText(line.replace("{count}", String.valueOf(commands.size())))
                        .build(player));
            }

            if (!commands.isEmpty()) {
                lore.add("");
                int shown = Math.min(commands.size(), 3);
                for (int i = 0; i < shown; i++) {
                    lore.add(Text.createText("&7- &e/" + commands.get(i)).build(player));
                }
                if (commands.size() > 3) {
                    lore.add(Text.createText("&7... and " + (commands.size() - 3) + " more").build(player));
                }
            }

            lore.add("");
            for (String line : guiConfig.getStringList("guis."+guiId+".items.command-rewards.lore-footer")) {
                lore.add(Text.createText(line).build(player));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editCommandRewards());
    }

    private void updateProgressiveMultiplier() {
        int slot = guiConfig.getInt("guis."+guiId+".items.progressive-multiplier.slot", 22);
        double multiplier = arena.getConfig().getRewardConfig().getProgressiveMultiplier();

        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.progressive-multiplier.material", "EXPERIENCE_BOTTLE")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.progressive-multiplier.name", "&a&lProgressive Multiplier")).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.progressive-multiplier.lore")) {
                String processed = line
                        .replace("{multiplier}", String.format("%.1f", multiplier * 100))
                        .replace("{example}", String.format("%.2f", 100 * multiplier));
                lore.add(Text.createText(processed).build(player));
            }
            meta.setLore(lore);

            meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editProgressiveMultiplier());
    }

    private String getRewardTypeDescription(RewardType type) {
        String key = "reward-type-desc-" + type.name().toLowerCase();
        return guiConfig.getString("guis."+guiId+".descriptions." + key, type.getDisplayName());
    }

    private void toggleRewardStatus() {
        boolean newValue = !arena.getConfig().getRewardConfig().isEnabled();

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".rewards.enabled", newValue);
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.reward-status-toggled")
                        .replace("{status}", newValue ? "enabled" : "disabled").build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    private void cycleRewardType() {
        RewardType current = arena.getConfig().getRewardConfig().getType();
        RewardType[] types = RewardType.values();
        int nextIndex = (current.ordinal() + 1) % types.length;
        RewardType next = types[nextIndex];

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".rewards.type", next.name());
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.reward-type-changed")
                        .replace("{type}", next.getDisplayName()).build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    private void editMoneyReward() {
        close();

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.money-reward").build())
                .withValidator(InputValidators.arenaId())
                .withInvalidMessage(Text.createTextWithLang("prompts.money-reward-invalid").build())
                .onComplete(input -> {
                    try {
                        double amount = Double.parseDouble(input);

                        if (amount < 0) {
                            player.sendMessage(Text.createTextWithLang("prompts.money-reward-invalid").build());
                            reopenGUI();
                            return;
                        }

                        plugin.getFileManager().getArenas()
                                .set("arenas." + arenaId + ".rewards.money", amount);
                        plugin.getFileManager().getArenas().save();
                        reopenGUI();

                    } catch (NumberFormatException e) {
                        player.sendMessage(
                                Text.createTextWithLang("prompts.money-reward-invalid").build());
                        reopenGUI();
                    }
                })
                .onCancel(() -> reopenGUI())
                .start();
    }

    private void editItemRewards() {
        close();

        player.sendMessage(Text.createTextWithLang("prompts.invalid-item-format").build(player));
        collectItemRewards(new ArrayList<>());
    }

    private void collectItemRewards(List<String> items) {
        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.item-rewards-info").build())
                .withValidator(InputValidators.arenaId())
                .withCancelMessage(Text.createTextWithLang("prompts.cancelled-message").build())
                .onComplete(input -> {
                    if (input.equalsIgnoreCase("done")) {
                        plugin.getFileManager().getArenas()
                                .set("arenas." + arenaId + ".rewards.items", items);
                        plugin.getFileManager().getArenas().save();

                        player.sendMessage(Text.createTextWithLang("prompts.item-rewards-updated")
                                        .replace("{count}", String.valueOf(items.size())).build(player));

                        reopenGUI();
                        return;
                    }

                    if (input.equalsIgnoreCase("cancel")) {
                        player.sendMessage(Text.createTextWithLang("prompts.cancelled-message").build());
                        reopenGUI();
                        return;
                    }

                    // Validate format: MATERIAL AMOUNT
                    String[] parts = input.split(" ");
                    if (parts.length != 2) {
                        player.sendMessage(Text.createTextWithLang("prompts.invalid-item-format").build());
                        collectItemRewards(items);
                        return;
                    }

                    try {
                        Material.valueOf(parts[0].toUpperCase());
                        Integer.parseInt(parts[1]);

                        items.add(input.toUpperCase());
                        player.sendMessage(Text.createTextWithLang("prompts.item-added")
                                        .replace("{item}", input.toUpperCase())
                                        .replace("{count}", String.valueOf(items.size())).build(player));

                        collectItemRewards(items);

                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Text.createTextWithLang("prompts.invalid-material").build());
                        collectItemRewards(items);
                    }
                })
                .onCancel(this::reopenGUI)
                .start();
    }

    private void editCommandRewards() {
        close();

        collectCommandRewards(new ArrayList<>());
    }

    private void collectCommandRewards(List<String> commands) {
        plugin.getChatInputManager().requestInput(player)
                .withValidator(InputValidators.arenaId())
                .withCancelMessage(Text.createTextWithLang("prompts.cancelled-message").build())
                .withPrompt(Text.createTextWithLang("prompts.command-rewards-prompt").build())
                .onComplete(input -> {
                    if (input.equalsIgnoreCase("done")) {
                        plugin.getFileManager().getArenas()
                                .set("arenas." + arenaId + ".rewards.commands", commands);
                        plugin.getFileManager().getArenas().save();

                        player.sendMessage(Text.createTextWithLang("prompts.command-rewards-updated")
                                        .replace("{count}", String.valueOf(commands.size())).build(player));

                        reopenGUI();
                        return;
                    }

                    if (input.equalsIgnoreCase("cancel")) {
                        player.sendMessage(Text.createTextWithLang("prompts.cancelled-message").build());
                        reopenGUI();
                        return;
                    }

                    // Remove leading slash if present
                    String command = input.startsWith("/") ? input.substring(1) : input;

                    commands.add(command);
                    player.sendMessage(Text.createTextWithLang("prompts.command-added")
                                    .replace("{command}", command)
                                    .replace("{count}", String.valueOf(commands.size())).build(player));

                    collectCommandRewards(commands);
                })
                .start();
    }

    private void editProgressiveMultiplier() {
        close();

        plugin.getChatInputManager().requestInput(player)
                .withValidator(InputValidators.arenaId())
                .withCancelMessage(Text.createTextWithLang("prompts.cancelled-message").build())
                .withPrompt(Text.createTextWithLang("prompts.multiplier-prompt").build())
                .onComplete(input -> {
                    try {
                        double value = Double.parseDouble(input);

                        if (value < 0 || value > 1) {
                            player.sendMessage(Text.createTextWithLang("prompts.invalid-multiplier").build(player));
                            reopenGUI();
                            return;
                        }

                        plugin.getFileManager().getArenas()
                                .set("arenas." + arenaId + ".rewards.progressive-multiplier", value);
                        plugin.getFileManager().getArenas().save();

                        player.sendMessage(Text.createTextWithLang("prompts.multiplier-updated")
                                        .replace("{value}", String.format("%.1f", value * 100)).build(player));

                        reopenGUI();

                    } catch (NumberFormatException e) {
                        player.sendMessage(Text.createTextWithLang("prompts.invalid-number").build(player));
                        reopenGUI();
                    }
                })
                .start();
    }

    @Override
    protected void handleCustomAction(int slot, String actionType, String actionValue, String itemId) {
        if (actionType.equals("clear-items")) {
            plugin.getFileManager().getArenas()
                    .set("arenas." + arenaId + ".rewards.items", new ArrayList<>());
            plugin.getFileManager().getArenas().save();

            player.sendMessage(Text.createTextWithLang("prompts.items-cleared").build(player));

            playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
            reopenGUI();

        } else if (actionType.equals("clear-commands")) {
            plugin.getFileManager().getArenas()
                    .set("arenas." + arenaId + ".rewards.commands", new ArrayList<>());
            plugin.getFileManager().getArenas().save();

            player.sendMessage(Text.createTextWithLang("prompts.commands-cleared").build(player));

            playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
            reopenGUI();
        }
    }

    private void reopenGUI() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getArenaManager().reloadArenas();
            Arena reloaded = plugin.getArenaManager().getArena(arenaId);
            if (reloaded != null) {
                new RewardEditorGUI(plugin, player, reloaded).open();
            }
        }, 1L);
    }

    @Override
    protected void onBack() {
        new ArenaEditorGUI(plugin, player, arena).open();
    }
}