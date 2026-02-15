package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.config.DeathAction;
import me.bixgamer707.hordes.config.ItemDropMode;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Mode settings GUI - 100% configurable
 * Configure survival mode, death handling, item handling
 */
public class ModeSettingsGUI extends BaseGUI {

    private final Arena arena;
    private final String arenaId;

    public ModeSettingsGUI(Hordes plugin, Player player, Arena arena) {
        super(plugin, player, "admin-mode-settings");
        this.arena = arena;
        this.arenaId = arena.getId();
    }

    @Override
    protected void buildDynamic() {
        updateSurvivalMode();
        updateDeathAction();
        updateItemDropMode();
        updatePvPSetting();
        updateKeepInventory();
    }

    private void updateSurvivalMode() {
        int slot = guiConfig.getInt("guis."+guiId+".items.survival-mode.slot", 10);
        boolean enabled = arena.getConfig().getSurvivalMode().isEnabled();
        
        String materialKey = enabled ? "material-enabled" : "material-disabled";
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.survival-mode." + materialKey, enabled ? "GRASS_BLOCK" : "BEDROCK")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.survival-mode.name", "&a&lSurvival Mode")
                .replace("{status}", enabled ? "&aEnabled" : "&cDisabled");
            meta.setDisplayName(Text.createText(name).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.survival-mode.lore")) {
                String processed = line
                    .replace("{status}", enabled ? "&aEnabled" : "&cDisabled")
                    .replace("{mode}", enabled ? "Dungeon" : "Arena");
                lore.add(Text.createText(processed).build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> toggleSurvivalMode());
    }

    private void updateDeathAction() {
        int slot = guiConfig.getInt("guis."+guiId+".items.death-action.slot", 12);
        DeathAction action = arena.getConfig().getDeathHandling().getAction();
        
        String materialKey = "material-" + action.name().toLowerCase();
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.death-action." + materialKey, "SKELETON_SKULL")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.death-action.name", "&c&lDeath Action")).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.death-action.lore")) {
                String processed = line
                    .replace("{action}", action.getDisplayName())
                    .replace("{action_desc}", getDeathActionDescription(action));
                lore.add(Text.createText(processed).build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> cycleDeathAction());
    }

    private void updateItemDropMode() {
        int slot = guiConfig.getInt("guis."+guiId+".items.item-drop-mode.slot", 14);
        ItemDropMode mode = arena.getConfig().getItemHandling().getDropMode();
        
        String materialKey = "material-" + mode.name().toLowerCase();
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.item-drop-mode." + materialKey, "CHEST")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.item-drop-mode.name", "&6&lItem Drop Mode")).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.item-drop-mode.lore")) {
                String processed = line
                    .replace("{mode}", mode.getDisplayName())
                    .replace("{mode_desc}", getDropModeDescription(mode));
                lore.add(Text.createText(processed).build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> cycleItemDropMode());
    }

    private void updatePvPSetting() {
        int slot = guiConfig.getInt("guis."+guiId+".items.pvp-setting.slot", 16);
        boolean allowed = arena.getConfig().getSurvivalMode().isPvPAllowed();
        
        String materialKey = allowed ? "material-enabled" : "material-disabled";
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.pvp-setting." + materialKey,
                allowed ? "DIAMOND_SWORD" : "WOODEN_SWORD")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.pvp-setting.name", "&e&lPvP")
                .replace("{status}", allowed ? "&aAllowed" : "&cDisabled");
            meta.setDisplayName(Text.createText(name).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.pvp-setting.lore")) {
                lore.add(Text.createText(line.replace("{status}", allowed ? "&aAllowed" : "&cDisabled"))
                    .build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> togglePvP());
    }

    private void updateKeepInventory() {
        int slot = guiConfig.getInt("guis."+guiId+".items.keep-inventory.slot", 22);
        boolean keep = arena.getConfig().getItemHandling().shouldKeepInventory();
        
        String materialKey = keep ? "material-enabled" : "material-disabled";
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.keep-inventory." + materialKey,
                keep ? "ENDER_CHEST" : "CHEST")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.keep-inventory.name", "&b&lKeep Inventory")
                .replace("{status}", keep ? "&aEnabled" : "&cDisabled");
            meta.setDisplayName(Text.createText(name).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.keep-inventory.lore")) {
                lore.add(Text.createText(line.replace("{status}", keep ? "&aEnabled" : "&cDisabled"))
                    .build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> toggleKeepInventory());
    }

    private String getDeathActionDescription(DeathAction action) {
        String key = "death-action-desc-" + action.name().toLowerCase();
        return guiConfig.getString("guis."+guiId+".descriptions." + key, action.getDisplayName());
    }

    private String getDropModeDescription(ItemDropMode mode) {
        String key = "drop-mode-desc-" + mode.name().toLowerCase();
        return guiConfig.getString("guis."+guiId+".descriptions." + key, mode.getDisplayName());
    }

    private void toggleSurvivalMode() {
        boolean newValue = !arena.getConfig().getSurvivalMode().isEnabled();
        
        plugin.getFileManager().getArenas()
            .set("arenas." + arenaId + ".survival-mode.enabled", newValue);
        plugin.getFileManager().getArenas().save();
        
        player.sendMessage(Text.createTextWithLang(
            "admin.survival-mode-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));
        
        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    private void cycleDeathAction() {
        DeathAction current = arena.getConfig().getDeathHandling().getAction();
        DeathAction[] actions = DeathAction.values();
        int nextIndex = (current.ordinal() + 1) % actions.length;
        DeathAction next = actions[nextIndex];
        
        plugin.getFileManager().getArenas()
            .set("arenas." + arenaId + ".death-handling.action", next.name());
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang(
                        "admin.death-action-changed")
                .replace("{action}", next.getDisplayName()).build(player));
        
        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    private void cycleItemDropMode() {
        ItemDropMode current = arena.getConfig().getItemHandling().getDropMode();
        ItemDropMode[] modes = ItemDropMode.values();
        int nextIndex = (current.ordinal() + 1) % modes.length;
        ItemDropMode next = modes[nextIndex];
        
        plugin.getFileManager().getArenas()
            .set("arenas." + arenaId + ".item-handling.drop-mode", next.name());
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang(
                        "admin.item-drop-mode-changed")
                .replace("{action}", next.getDisplayName()).build(player));
        
        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    private void togglePvP() {
        boolean newValue = !arena.getConfig().getSurvivalMode().isPvPAllowed();
        
        plugin.getFileManager().getArenas()
            .set("arenas." + arenaId + ".survival-mode.allow-pvp", newValue);
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang(
                        "admin.pvp-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));
        
        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    private void toggleKeepInventory() {
        boolean newValue = !arena.getConfig().getItemHandling().shouldKeepInventory();
        
        plugin.getFileManager().getArenas()
            .set("arenas." + arenaId + ".item-handling.keep-inventory-on-death", newValue);
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang(
                        "admin.keep-inventory-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));
        
        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    private void reopenGUI() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getArenaManager().reloadArenas();
            Arena reloaded = plugin.getArenaManager().getArena(arenaId);
            if (reloaded != null) {
                new ModeSettingsGUI(plugin, player, reloaded).open();
            }
        }, 1L);
    }

    @Override
    protected void onBack() {
        new ArenaEditorGUI(plugin, player, arena).open();
    }
}