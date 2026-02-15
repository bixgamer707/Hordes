package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import me.bixgamer707.hordes.utils.InputValidators;
import org.bukkit.Material;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Player settings GUI - 100% configurable
 * Edit min/max players, countdown, auto-start
 */
public class PlayerSettingsGUI extends BaseGUI {

    private final Arena arena;
    private final String arenaId;

    public PlayerSettingsGUI(Hordes plugin, Player player, Arena arena) {
        super(plugin, player, "admin-player-settings");
        this.arena = arena;
        this.arenaId = arena.getId();
    }

    @Override
    protected void buildDynamic() {
        updateMinPlayers();
        updateMaxPlayers();
        updateCountdown();
        updateAutoStart();
    }

    private void updateMinPlayers() {
        int slot = guiConfig.getInt("guis."+guiId+".items.min-players.slot", 11);
        int minPlayers = arena.getConfig().getMinPlayers();
        
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.min-players.material", "PLAYER_HEAD")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.min-players.name", "&a&lMinimum Players")).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.min-players.lore")) {
                lore.add(Text.createText(line.replace("{min_players}", String.valueOf(minPlayers)))
                    .build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editMinPlayers());
    }

    private void updateMaxPlayers() {
        int slot = guiConfig.getInt("guis."+guiId+".items.max-players.slot", 13);
        int maxPlayers = arena.getConfig().getMaxPlayers();
        
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.max-players.material", "PLAYER_HEAD")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.max-players.name", "&c&lMaximum Players")).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.max-players.lore")) {
                lore.add(Text.createText(line.replace("{max_players}", String.valueOf(maxPlayers)))
                    .build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editMaxPlayers());
    }

    private void updateCountdown() {
        int slot = guiConfig.getInt("guis."+guiId+".items.countdown.slot", 15);
        int countdown = arena.getConfig().getCountdownTime();
        
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.countdown.material", "CLOCK")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.countdown.name", "&e&lCountdown Time")).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.countdown.lore")) {
                lore.add(Text.createText(line.replace("{countdown}", String.valueOf(countdown)))
                    .build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editCountdown());
    }

    private void updateAutoStart() {
        int slot = guiConfig.getInt("guis."+guiId+".items.auto-start.slot", 22);
        boolean autoStart = arena.getConfig().isAutoStart();
        
        String materialKey = autoStart ? "material-enabled" : "material-disabled";
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.auto-start." + materialKey, autoStart ? "EMERALD" : "REDSTONE")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.auto-start.name", "&6&lAuto-Start")
                .replace("{status}", autoStart ? "&aEnabled" : "&cDisabled");
            meta.setDisplayName(Text.createText(name).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.auto-start.lore")) {
                lore.add(Text.createText(line.replace("{status}", autoStart ? "&aEnabled" : "&cDisabled"))
                    .build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> toggleAutoStart());
    }

    private void editMinPlayers() {
        close();
        int maxPlayers = arena.getConfig().getMaxPlayers();

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang(
                                "prompts.min-players").build(player))
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-min-players").build())
                .withValidator(InputValidators.arenaId())
                .onComplete(input -> {
                    int value;
                    try {
                        value = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Text.createTextWithLang(
                                        "prompts.invalid-min-players")
                                .replace("{max}", String.valueOf(maxPlayers)).build(player));
                        return;
                    }

                    if (value < 1 || value > maxPlayers) {
                        player.sendMessage(Text.createTextWithLang("prompts.invalid-min-players")
                                .replace("{max}", String.valueOf(maxPlayers)).build());
                        return;
                    }

                    plugin.getFileManager().getArenas()
                            .set("arenas." + arenaId + ".min-players", value);
                    plugin.getFileManager().getArenas().save();

                    player.sendMessage(Text.createTextWithLang("prompts.min-players-updated")
                            .replace("{value}", String.valueOf(value)).build());

                    reopenGUI();
                })
                .onCancel(this::reopenGUI)
                .start();

    }

    private void editMaxPlayers() {
        close();

        int minPlayers = arena.getConfig().getMinPlayers();

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang(
                                "prompts.max-players").build(player))
                .withValidator(InputValidators.arenaId())
                .onComplete(input -> {
                    int value;
                    try {
                        value = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Text.createTextWithLang(
                                        "prompts.invalid-max-players")
                                .replace("{min}", String.valueOf(minPlayers)).build(player));
                        return;
                    }

                    if (value < minPlayers || value > 100) {
                        player.sendMessage(Text.createTextWithLang("prompts.invalid-max-players")
                                .replace("{min}", String.valueOf(minPlayers)).build(player));
                        return;
                    }

                    plugin.getFileManager().getArenas()
                            .set("arenas." + arenaId + ".max-players", value);
                    plugin.getFileManager().getArenas().save();

                    player.sendMessage(Text.createTextWithLang("prompts.max-players-updated")
                            .replace("{value}", String.valueOf(value)).build(player));

                    reopenGUI();
                })
                .onCancel(this::reopenGUI)
                .start();
    }

    private void editCountdown() {
        close();
        
        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang(
                                "prompts.countdown-time").build(player))
                .withValidator(InputValidators.arenaId())
                .onComplete(input -> {
                    int value;
                    try {
                        value = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Text.createTextWithLang(
                                        "prompts.invalid-countdown")
                                .build(player));
                        return;
                    }

                    if (value < 0 || value > 500) {
                        player.sendMessage(Text.createTextWithLang("prompts.invalid-countdown")
                                .build(player));
                        return;
                    }

                    plugin.getFileManager().getArenas()
                            .set("arenas." + arenaId + ".countdown", value);
                    plugin.getFileManager().getArenas().save();

                    player.sendMessage(Text.createTextWithLang("prompts.countdown-time-updated")
                            .replace("{value}", String.valueOf(value)).build(player));

                    reopenGUI();
                })
                .onCancel(this::reopenGUI)
                .start();
    }

    private void toggleAutoStart() {
        boolean newValue = !arena.getConfig().isAutoStart();
        
        plugin.getFileManager().getArenas()
            .set("arenas." + arenaId + ".auto-start", newValue);
        plugin.getFileManager().getArenas().save();
        
        player.sendMessage(Text.createTextWithLang("prompts.auto-start-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));
        
        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    private void reopenGUI() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getArenaManager().reloadArenas();
            Arena reloaded = plugin.getArenaManager().getArena(arenaId);
            if (reloaded != null) {
                new PlayerSettingsGUI(plugin, player, reloaded).open();
            }
        }, 1L);
    }

    @Override
    protected void onBack() {
        new ArenaEditorGUI(plugin, player, arena).open();
    }
}