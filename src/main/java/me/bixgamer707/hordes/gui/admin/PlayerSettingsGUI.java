package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.config.WaveProgressionType;
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
 * Player settings GUI - 100% configurable
 * Edit min/max players, countdown, auto-start, wave-delay, cooldown,
 * wave-progression type and global-cooldown.
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
        updateWaveDelay();
        updateCooldown();
        updateWaveProgression();
        updateGlobalCooldown();
    }

    // ==========================================
    // MIN / MAX PLAYERS
    // ==========================================

    private void updateMinPlayers() {
        int slot = guiConfig.getInt("guis."+guiId+".items.min-players.slot", 10);
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
        int slot = guiConfig.getInt("guis."+guiId+".items.max-players.slot", 12);
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

    /**
     * FIX: antes usaba InputValidators.arenaId() (regex ^[a-z0-9_]+$), que
     * rechaza cualquier número escrito normalmente. Se sustituye por
     * positiveInteger(), que exige solo dígitos y > 0.
     */
    private void editMinPlayers() {
        close();
        int maxPlayers = arena.getConfig().getMaxPlayers();

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.min-players").build(player))
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-min-players")
                        .replace("{max}", String.valueOf(maxPlayers)).build())
                .withValidator(InputValidators.positiveInteger())
                .onComplete(input -> {
                    int value = Integer.parseInt(input.trim());

                    if (value > maxPlayers) {
                        player.sendMessage(Text.createTextWithLang("prompts.invalid-min-players")
                                .replace("{max}", String.valueOf(maxPlayers)).build(player));
                        reopenGUI();
                        return;
                    }

                    plugin.getFileManager().getArenas()
                            .set("arenas." + arenaId + ".min-players", value);
                    plugin.getFileManager().getArenas().save();

                    player.sendMessage(Text.createTextWithLang("prompts.min-players-updated")
                            .replace("{0}", String.valueOf(value)).build(player));

                    reopenGUI();
                })
                .onCancel(this::reopenGUI)
                .start();
    }

    private void editMaxPlayers() {
        close();
        int minPlayers = arena.getConfig().getMinPlayers();

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.max-players").build(player))
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-max-players")
                        .replace("{min}", String.valueOf(minPlayers)).build())
                .withValidator(InputValidators.positiveInteger())
                .onComplete(input -> {
                    int value = Integer.parseInt(input.trim());

                    if (value < minPlayers || value > 100) {
                        player.sendMessage(Text.createTextWithLang("prompts.invalid-max-players")
                                .replace("{min}", String.valueOf(minPlayers)).build(player));
                        reopenGUI();
                        return;
                    }

                    plugin.getFileManager().getArenas()
                            .set("arenas." + arenaId + ".max-players", value);
                    plugin.getFileManager().getArenas().save();

                    player.sendMessage(Text.createTextWithLang("prompts.max-players-updated")
                            .replace("{0}", String.valueOf(value)).build(player));

                    reopenGUI();
                })
                .onCancel(this::reopenGUI)
                .start();
    }

    // ==========================================
    // COUNTDOWN / AUTO-START
    // ==========================================

    private void updateCountdown() {
        int slot = guiConfig.getInt("guis."+guiId+".items.countdown.slot", 14);
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

    private void editCountdown() {
        close();

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.countdown-time").build(player))
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-countdown").build())
                .withValidator(InputValidators.integerRange(0, 500))
                .onComplete(input -> {
                    int value = Integer.parseInt(input.trim());

                    plugin.getFileManager().getArenas()
                            .set("arenas." + arenaId + ".countdown-time", value);
                    plugin.getFileManager().getArenas().save();

                    player.sendMessage(Text.createTextWithLang("prompts.countdown-time-updated")
                            .replace("{0}", String.valueOf(value)).build(player));

                    reopenGUI();
                })
                .onCancel(this::reopenGUI)
                .start();
    }

    private void updateAutoStart() {
        int slot = guiConfig.getInt("guis."+guiId+".items.auto-start.slot", 16);
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

    // ==========================================
    // WAVE DELAY
    // ==========================================

    private void updateWaveDelay() {
        int slot = guiConfig.getInt("guis."+guiId+".items.wave-delay.slot", 19);
        int seconds = arena.getConfig().getWaveDelay();

        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.wave-delay.material", "REPEATER")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.wave-delay.name", "&e&lWave Delay")).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.wave-delay.lore")) {
                lore.add(Text.createText(line.replace("{seconds}", String.valueOf(seconds))).build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editWaveDelay());
    }

    private void editWaveDelay() {
        close();

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.wave-delay").build(player))
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-number").build())
                .withValidator(InputValidators.integerRange(0, 3600))
                .onComplete(input -> {
                    int value = Integer.parseInt(input.trim());

                    plugin.getFileManager().getArenas()
                            .set("arenas." + arenaId + ".wave-delay", value);
                    plugin.getFileManager().getArenas().save();

                    player.sendMessage(Text.createTextWithLang("prompts.wave-delay-updated")
                            .replace("{0}", String.valueOf(value)).build(player));

                    reopenGUI();
                })
                .onCancel(this::reopenGUI)
                .start();
    }

    // ==========================================
    // ARENA COOLDOWN
    // ==========================================

    private void updateCooldown() {
        int slot = guiConfig.getInt("guis."+guiId+".items.cooldown.slot", 21);
        long seconds = arena.getConfig().getCooldownDuration();

        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.cooldown.material", "CLOCK")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Text.createText(
                    guiConfig.getString("guis."+guiId+".items.cooldown.name", "&e&lArena Cooldown")).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.cooldown.lore")) {
                lore.add(Text.createText(line.replace("{seconds}", String.valueOf(seconds))).build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editCooldown());
    }

    private void editCooldown() {
        close();

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.cooldown").build(player))
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-number").build())
                .withValidator(InputValidators.integerRange(0, 604800))
                .onComplete(input -> {
                    long value = Long.parseLong(input.trim());

                    plugin.getFileManager().getArenas()
                            .set("arenas." + arenaId + ".cooldown", value);
                    plugin.getFileManager().getArenas().save();

                    player.sendMessage(Text.createTextWithLang("prompts.cooldown-updated")
                            .replace("{0}", String.valueOf(value)).build(player));

                    reopenGUI();
                })
                .onCancel(this::reopenGUI)
                .start();
    }

    // ==========================================
    // WAVE PROGRESSION TYPE
    // ==========================================

    private void updateWaveProgression() {
        int slot = guiConfig.getInt("guis."+guiId+".items.wave-progression.slot", 23);
        WaveProgressionType type = arena.getConfig().getProgressionType();

        String materialKey = "material-" + type.name().toLowerCase();
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.wave-progression." + materialKey, "COMPARATOR")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.wave-progression.name", "&d&lWave Progression")
                    .replace("{type}", type.name());
            meta.setDisplayName(Text.createText(name).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.wave-progression.lore")) {
                lore.add(Text.createText(line).build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> cycleWaveProgression());
    }

    private void cycleWaveProgression() {
        WaveProgressionType current = arena.getConfig().getProgressionType();
        WaveProgressionType[] types = WaveProgressionType.values();
        WaveProgressionType next = types[(current.ordinal() + 1) % types.length];

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".wave-progression", next.name());
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("prompts.wave-progression-changed")
                .replace("{0}", next.name()).build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================
    // GLOBAL COOLDOWN
    // ==========================================

    private void updateGlobalCooldown() {
        int slot = guiConfig.getInt("guis."+guiId+".items.global-cooldown.slot", 25);
        boolean enabled = arena.getConfig().isGlobalCooldown();

        String materialKey = enabled ? "material-enabled" : "material-disabled";
        ItemStack item = new ItemStack(Material.valueOf(
                guiConfig.getString("guis."+guiId+".items.global-cooldown." + materialKey,
                        enabled ? "NETHERITE_INGOT" : "IRON_INGOT")));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = guiConfig.getString("guis."+guiId+".items.global-cooldown.name", "&b&lGlobal Cooldown")
                    .replace("{status}", enabled ? "&aEnabled" : "&cDisabled");
            meta.setDisplayName(Text.createText(name).build(player));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("guis."+guiId+".items.global-cooldown.lore")) {
                lore.add(Text.createText(line.replace("{status}", enabled ? "&aEnabled" : "&cDisabled"))
                        .build(player));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> toggleGlobalCooldown());
    }

    private void toggleGlobalCooldown() {
        boolean newValue = !arena.getConfig().isGlobalCooldown();

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".global-cooldown", newValue);
        plugin.getFileManager().getArenas().save();

        player.sendMessage(Text.createTextWithLang("prompts.global-cooldown-toggled")
                .replace("{status}", newValue ? "enabled" : "disabled").build(player));

        playSound(guiConfig.getString("guis."+guiId+".sounds.click", "UI_BUTTON_CLICK"));
        reopenGUI();
    }

    // ==========================================

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