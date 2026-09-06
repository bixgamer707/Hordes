package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.config.DeathAction;
import me.bixgamer707.hordes.config.ItemDropMode;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import me.bixgamer707.hordes.utils.InputValidators;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Mode settings GUI - 100% configurable
 * Configure survival mode, death handling, item handling and PvP.
 * Only exposes fields that actually have an effect in-game (dead/no-op
 * config fields like death-handling.can-rejoin and .spectate-on-death are
 * intentionally left out to avoid misleading toggles).
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
        updatePvPSetting();
        updateClearInventory();
        updateGameModeForce();
        updateDeathAction();
        updateKeepInventory();
        updateItemDropMode();
        updateDropItemsOnDeath();
        updateTeleportOnDeath();
        updateSpectateOnDeath();
        updateRejoinCooldown();
        updateCanRejoin();
    }

    // ==========================================
    // SURVIVAL MODE
    // ==========================================

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

    private void toggleSurvivalMode() {
        boolean newValue = !arena.getConfig().getSurvivalMode().isEnabled();

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".survival-mode.enabled", newValue);
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.survival-mode-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================
    // PVP
    // ==========================================

    private void updatePvPSetting() {
        int slot = guiConfig.getInt("guis."+guiId+".items.pvp-setting.slot", 12);
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

    private void togglePvP() {
        boolean newValue = !arena.getConfig().getSurvivalMode().isPvPAllowed();

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".survival-mode.allow-pvp", newValue);
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.pvp-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================
    // CLEAR INVENTORY ON JOIN
    // ==========================================

    private void updateClearInventory() {
        int slot = guiConfig.getInt("guis."+guiId+".items.clear-inventory.slot", 14);
        boolean enabled = arena.getConfig().getSurvivalMode().shouldClearInventory();

        String materialKey = enabled ? "material-enabled" : "material-disabled";
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.clear-inventory." + materialKey,
                        enabled ? "LAVA_BUCKET" : "WATER_BUCKET")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.clear-inventory.name", "&6&lClear Inventory On Join")
                    .replace("{status}", enabled ? "&aEnabled" : "&cDisabled");
            meta.setDisplayName(Text.createText(name).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.clear-inventory.lore")) {
                lore.add(Text.createText(line.replace("{status}", enabled ? "&aEnabled" : "&cDisabled"))
                        .build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> toggleClearInventory());
    }

    private void toggleClearInventory() {
        boolean newValue = !arena.getConfig().getSurvivalMode().shouldClearInventory();

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".survival-mode.clear-inventory", newValue);
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.clear-inventory-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================
    // FORCED GAMEMODE (combines force-gamemode + gamemode into one cycle)
    // ==========================================

    private void updateGameModeForce() {
        int slot = guiConfig.getInt("guis."+guiId+".items.gamemode-force.slot", 16);
        boolean forced = arena.getConfig().getSurvivalMode().shouldForceGameMode();
        GameMode mode = arena.getConfig().getSurvivalMode().getGameMode();

        String materialKey = !forced ? "material-off" : "material-" + mode.name().toLowerCase();
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.gamemode-force." + materialKey, "BARRIER")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String display = !forced ? "OFF" : mode.name();
            String name = guiConfig.getString("guis."+guiId+".items.gamemode-force.name", "&d&lForced Gamemode")
                    .replace("{mode}", display);
            meta.setDisplayName(Text.createText(name).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.gamemode-force.lore")) {
                lore.add(Text.createText(line).build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> cycleGameModeForce());
    }

    /**
     * Cycles OFF -> SURVIVAL -> ADVENTURE -> CREATIVE -> OFF, writing both
     * force-gamemode and gamemode together so they never end up inconsistent.
     */
    private void cycleGameModeForce() {
        boolean forced = arena.getConfig().getSurvivalMode().shouldForceGameMode();
        GameMode mode = arena.getConfig().getSurvivalMode().getGameMode();

        boolean newForced;
        GameMode newMode;

        if (!forced) {
            newForced = true;
            newMode = GameMode.SURVIVAL;
        } else if (mode == GameMode.SURVIVAL) {
            newForced = true;
            newMode = GameMode.ADVENTURE;
        } else if (mode == GameMode.ADVENTURE) {
            newForced = true;
            newMode = GameMode.CREATIVE;
        } else {
            newForced = false;
            newMode = GameMode.SURVIVAL;
        }

        plugin.getFileManager().getArenas().set("arenas." + arenaId + ".survival-mode.force-gamemode", newForced);
        plugin.getFileManager().getArenas().set("arenas." + arenaId + ".survival-mode.gamemode", newMode.name());
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.gamemode-force-changed")
                .replace("{mode}", newForced ? newMode.name() : "OFF").build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================
    // DEATH ACTION
    // ==========================================

    private void updateDeathAction() {
        int slot = guiConfig.getInt("guis."+guiId+".items.death-action.slot", 19);
        DeathAction action = arena.getConfig().getDeathHandling().getAction();

        String materialKey = "material-" + action.name().toLowerCase();
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.death-action." + materialKey, "SKELETON_SKULL")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.death-action.name", "&c&lDeath Action")
                    .replace("{action}", action.getDisplayName());
            meta.setDisplayName(Text.createText(name).build(player));

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

    private String getDeathActionDescription(DeathAction action) {
        String key = "death-action-desc-" + action.name().toLowerCase();
        return guiConfig.getString("guis."+guiId+".descriptions." + key, action.getDisplayName());
    }

    private void cycleDeathAction() {
        DeathAction current = arena.getConfig().getDeathHandling().getAction();
        DeathAction[] actions = DeathAction.values();
        int nextIndex = (current.ordinal() + 1) % actions.length;
        DeathAction next = actions[nextIndex];

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".death-handling.action", next.name());
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.death-action-changed")
                .replace("{action}", next.getDisplayName()).build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================
    // KEEP INVENTORY
    // ==========================================

    private void updateKeepInventory() {
        int slot = guiConfig.getInt("guis."+guiId+".items.keep-inventory.slot", 21);
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

    private void toggleKeepInventory() {
        boolean newValue = !arena.getConfig().getItemHandling().shouldKeepInventory();

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".item-handling.keep-inventory-on-death", newValue);
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.keep-inventory-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================
    // ITEM DROP MODE
    // ==========================================

    private void updateItemDropMode() {
        int slot = guiConfig.getInt("guis."+guiId+".items.item-drop-mode.slot", 23);
        ItemDropMode mode = arena.getConfig().getItemHandling().getDropMode();

        String materialKey = "material-" + mode.name().toLowerCase();
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.item-drop-mode." + materialKey, "CHEST")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.item-drop-mode.name", "&6&lItem Drop Mode")
                    .replace("{mode}", mode.getDisplayName());
            meta.setDisplayName(Text.createText(name).build(player));

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

    private String getDropModeDescription(ItemDropMode mode) {
        String key = "drop-mode-desc-" + mode.name().toLowerCase();
        return guiConfig.getString("guis."+guiId+".descriptions." + key, mode.getDisplayName());
    }

    private void cycleItemDropMode() {
        ItemDropMode current = arena.getConfig().getItemHandling().getDropMode();
        ItemDropMode[] modes = ItemDropMode.values();
        int nextIndex = (current.ordinal() + 1) % modes.length;
        ItemDropMode next = modes[nextIndex];

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".item-handling.drop-mode", next.name());
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.item-drop-mode-changed")
                .replace("{mode}", next.getDisplayName()).build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================
    // DROP ITEMS ON DEATH
    // ==========================================

    private void updateDropItemsOnDeath() {
        int slot = guiConfig.getInt("guis."+guiId+".items.drop-items-on-death.slot", 25);
        boolean enabled = arena.getConfig().getItemHandling().shouldDropItems();

        String materialKey = enabled ? "material-enabled" : "material-disabled";
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.drop-items-on-death." + materialKey,
                        enabled ? "ITEM_FRAME" : "GLASS_PANE")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.drop-items-on-death.name", "&e&lDrop Items On Death")
                    .replace("{status}", enabled ? "&aEnabled" : "&cDisabled");
            meta.setDisplayName(Text.createText(name).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.drop-items-on-death.lore")) {
                lore.add(Text.createText(line.replace("{status}", enabled ? "&aEnabled" : "&cDisabled"))
                        .build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> toggleDropItemsOnDeath());
    }

    private void toggleDropItemsOnDeath() {
        boolean newValue = !arena.getConfig().getItemHandling().shouldDropItems();

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".item-handling.drop-items-on-death", newValue);
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.drop-items-on-death-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================
    // TELEPORT ON DEATH
    // ==========================================

    private void updateTeleportOnDeath() {
        int slot = guiConfig.getInt("guis."+guiId+".items.teleport-on-death.slot", 28);
        boolean enabled = arena.getConfig().getDeathHandling().shouldTeleport();

        String materialKey = enabled ? "material-enabled" : "material-disabled";
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.teleport-on-death." + materialKey,
                        enabled ? "ENDER_PEARL" : "COBWEB")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.teleport-on-death.name", "&d&lTeleport On Death")
                    .replace("{status}", enabled ? "&aEnabled" : "&cDisabled");
            meta.setDisplayName(Text.createText(name).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.teleport-on-death.lore")) {
                lore.add(Text.createText(line.replace("{status}", enabled ? "&aEnabled" : "&cDisabled"))
                        .build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> toggleTeleportOnDeath());
    }

    private void toggleTeleportOnDeath() {
        boolean newValue = !arena.getConfig().getDeathHandling().shouldTeleport();

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".death-handling.teleport-on-death", newValue);
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.teleport-on-death-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================
    // SPECTATE ON DEATH (REJOIN action)
    // ==========================================

    private void updateSpectateOnDeath() {
        int slot = guiConfig.getInt("guis."+guiId+".items.spectate-on-death.slot", 32);
        boolean enabled = arena.getConfig().getDeathHandling().shouldSpectate();

        String materialKey = enabled ? "material-enabled" : "material-disabled";
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.spectate-on-death." + materialKey,
                        enabled ? "ENDER_EYE" : "GRAY_DYE")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.spectate-on-death.name", "&5&lSpectate While Waiting")
                    .replace("{status}", enabled ? "&aEnabled" : "&cDisabled");
            meta.setDisplayName(Text.createText(name).build(player));

            List<String> lore = guiConfig.getStringList("guis."+guiId+".items.spectate-on-death.lore");
            List<String> processedLore = new ArrayList<>();
            if (lore.isEmpty()) {
                processedLore.add(Text.createText("&7Only applies to the &eREJOIN &7death action.").build(player));
                processedLore.add(Text.createText("&7If enabled, dead players become a").build(player));
                processedLore.add(Text.createText("&7spectator inside the arena while").build(player));
                processedLore.add(Text.createText("&7waiting on the rejoin cooldown,").build(player));
                processedLore.add(Text.createText("&7then automatically return to the fight.").build(player));
                processedLore.add("");
                processedLore.add(Text.createText("&7Status: " + (enabled ? "&aEnabled" : "&cDisabled")).build(player));
                processedLore.add("");
                processedLore.add(Text.createText("&eClick to toggle").build(player));
            } else {
                for (String line : lore) {
                    processedLore.add(Text.createText(line.replace("{status}", enabled ? "&aEnabled" : "&cDisabled"))
                            .build(player));
                }
            }
            meta.setLore(processedLore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> toggleSpectateOnDeath());
    }

    private void toggleSpectateOnDeath() {
        boolean newValue = !arena.getConfig().getDeathHandling().shouldSpectate();

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".death-handling.spectate-on-death", newValue);
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.spectate-on-death-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================
    // CAN REJOIN (KICK action)
    // ==========================================

    private void updateCanRejoin() {
        int slot = guiConfig.getInt("guis."+guiId+".items.can-rejoin.slot", 34);
        boolean enabled = arena.getConfig().getDeathHandling().canRejoin();

        String materialKey = enabled ? "material-enabled" : "material-disabled";
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.can-rejoin." + materialKey,
                        enabled ? "EXPERIENCE_BOTTLE" : "GLASS_BOTTLE")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.can-rejoin.name", "&b&lCan Rejoin After Kick")
                    .replace("{status}", enabled ? "&aEnabled" : "&cDisabled");
            meta.setDisplayName(Text.createText(name).build(player));

            List<String> lore = guiConfig.getStringList("guis."+guiId+".items.can-rejoin.lore");
            List<String> processedLore = new ArrayList<>();
            if (lore.isEmpty()) {
                processedLore.add(Text.createText("&7Only applies to the &eKICK &7death action.").build(player));
                processedLore.add(Text.createText("&7Disabled: dying is treated like a loss -").build(player));
                processedLore.add(Text.createText("&7the full arena cooldown applies.").build(player));
                processedLore.add(Text.createText("&7Enabled: only the (usually shorter)").build(player));
                processedLore.add(Text.createText("&7rejoin-cooldown applies instead.").build(player));
                processedLore.add("");
                processedLore.add(Text.createText("&7Status: " + (enabled ? "&aEnabled" : "&cDisabled")).build(player));
                processedLore.add("");
                processedLore.add(Text.createText("&eClick to toggle").build(player));
            } else {
                for (String line : lore) {
                    processedLore.add(Text.createText(line.replace("{status}", enabled ? "&aEnabled" : "&cDisabled"))
                            .build(player));
                }
            }
            meta.setLore(processedLore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> toggleCanRejoin());
    }

    private void toggleCanRejoin() {
        boolean newValue = !arena.getConfig().getDeathHandling().canRejoin();

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".death-handling.can-rejoin", newValue);
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("admin.can-rejoin-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================
    // REJOIN COOLDOWN
    // ==========================================

    private void updateRejoinCooldown() {
        int slot = guiConfig.getInt("guis."+guiId+".items.rejoin-cooldown.slot", 30);
        int seconds = arena.getConfig().getDeathHandling().getRejoinCooldown();

        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.rejoin-cooldown.material", "CLOCK")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.rejoin-cooldown.name", "&e&lRejoin Cooldown")).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.rejoin-cooldown.lore")) {
                lore.add(Text.createText(line.replace("{seconds}", String.valueOf(seconds))).build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editRejoinCooldown());
    }

    private void editRejoinCooldown() {
        close();
        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.rejoin-cooldown").build())
                .withValidator(InputValidators.integerRange(0, 86400))
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-number").build())
                .onComplete(input -> {
                    int value = Integer.parseInt(input.trim());
                    plugin.getFileManager().getArenas()
                            .set("arenas." + arenaId + ".death-handling.rejoin-cooldown", value);
                    plugin.getFileManager().getArenas().save();

                    player.sendMessage(Text.createTextWithLang("prompts.rejoin-cooldown-updated")
                            .replace("{0}", String.valueOf(value)).build(player));

                    reopenGUI();
                })
                .onCancel(() -> reopenGUI())
                .start();
    }

    // ==========================================

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