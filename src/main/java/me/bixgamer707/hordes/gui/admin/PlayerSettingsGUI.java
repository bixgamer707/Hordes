package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
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
        int slot = getConfigInt("items.min-players.slot", 11);
        int minPlayers = arena.getConfig().getMinPlayers();
        
        ItemStack item = new ItemStack(Material.valueOf(
            getConfigString("items.min-players.material", "PLAYER_HEAD")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(Text.createText(
                getConfigString("items.min-players.name", "&a&lMinimum Players")).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : getConfigStringList("items.min-players.lore")) {
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
        int slot = getConfigInt("items.max-players.slot", 13);
        int maxPlayers = arena.getConfig().getMaxPlayers();
        
        ItemStack item = new ItemStack(Material.valueOf(
            getConfigString("items.max-players.material", "PLAYER_HEAD")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(Text.createText(
                getConfigString("items.max-players.name", "&c&lMaximum Players")).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : getConfigStringList("items.max-players.lore")) {
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
        int slot = getConfigInt("items.countdown.slot", 15);
        int countdown = arena.getConfig().getCountdownTime();
        
        ItemStack item = new ItemStack(Material.valueOf(
            getConfigString("items.countdown.material", "CLOCK")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(Text.createText(
                getConfigString("items.countdown.name", "&e&lCountdown Time")).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : getConfigStringList("items.countdown.lore")) {
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
        int slot = getConfigInt("items.auto-start.slot", 22);
        boolean autoStart = arena.getConfig().isAutoStart();
        
        String materialKey = autoStart ? "material-enabled" : "material-disabled";
        ItemStack item = new ItemStack(Material.valueOf(
            getConfigString("items.auto-start." + materialKey, autoStart ? "EMERALD" : "REDSTONE")));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = getConfigString("items.auto-start.name", "&6&lAuto-Start")
                .replace("{status}", autoStart ? "&aEnabled" : "&cDisabled");
            meta.setDisplayName(Text.createText(name).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : getConfigStringList("items.auto-start.lore")) {
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
        
        new ConversationFactory(plugin)
            .withFirstPrompt(new NumericPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return Text.createText(getConfigString("prompts.min-players", 
                        "Enter minimum players (1-100):")).build();
                }
                
                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                    int value = input.intValue();
                    int maxPlayers = arena.getConfig().getMaxPlayers();
                    
                    if (value < 1 || value > maxPlayers) {
                        player.sendMessage(Text.createText(
                            getConfigString("messages.invalid-min-players", 
                                "&cMinimum must be between 1 and {max}")
                                .replace("{max}", String.valueOf(maxPlayers))).build(player));
                        return this;
                    }
                    
                    plugin.getFileManager().getArenas()
                        .set("arenas." + arenaId + ".min-players", value);
                    plugin.getFileManager().getArenas().save();
                    
                    player.sendMessage(Text.createText(
                        getConfigString("messages.min-players-updated", 
                            "&aMinimum players set to {value}")
                            .replace("{value}", String.valueOf(value))).build(player));
                    
                    return Prompt.END_OF_CONVERSATION;
                }
            })
            .withLocalEcho(false)
            .withTimeout(60)
            .addConversationAbandonedListener(event -> reopenGUI())
            .buildConversation(player)
            .begin();
    }

    private void editMaxPlayers() {
        close();
        
        new ConversationFactory(plugin)
            .withFirstPrompt(new NumericPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return Text.createText(getConfigString("prompts.max-players", 
                        "Enter maximum players (1-100):")).build();
                }
                
                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                    int value = input.intValue();
                    int minPlayers = arena.getConfig().getMinPlayers();
                    
                    if (value < minPlayers || value > 100) {
                        player.sendMessage(Text.createText(
                            getConfigString("messages.invalid-max-players", 
                                "&cMaximum must be between {min} and 100")
                                .replace("{min}", String.valueOf(minPlayers))).build(player));
                        return this;
                    }
                    
                    plugin.getFileManager().getArenas()
                        .set("arenas." + arenaId + ".max-players", value);
                    plugin.getFileManager().getArenas().save();
                    
                    player.sendMessage(Text.createText(
                        getConfigString("messages.max-players-updated", 
                            "&aMaximum players set to {value}")
                            .replace("{value}", String.valueOf(value))).build(player));
                    
                    return Prompt.END_OF_CONVERSATION;
                }
            })
            .withLocalEcho(false)
            .withTimeout(60)
            .addConversationAbandonedListener(event -> reopenGUI())
            .buildConversation(player)
            .begin();
    }

    private void editCountdown() {
        close();
        
        new ConversationFactory(plugin)
            .withFirstPrompt(new NumericPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return Text.createText(getConfigString("prompts.countdown", 
                        "Enter countdown time in seconds (5-60):")).build();
                }
                
                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                    int value = input.intValue();
                    
                    if (value < 5 || value > 60) {
                        player.sendMessage(Text.createText(
                            getConfigString("messages.invalid-countdown", 
                                "&cCountdown must be between 5 and 60 seconds")).build(player));
                        return this;
                    }
                    
                    plugin.getFileManager().getArenas()
                        .set("arenas." + arenaId + ".countdown-time", value);
                    plugin.getFileManager().getArenas().save();
                    
                    player.sendMessage(Text.createText(
                        getConfigString("messages.countdown-updated", 
                            "&aCountdown time set to {value} seconds")
                            .replace("{value}", String.valueOf(value))).build(player));
                    
                    return Prompt.END_OF_CONVERSATION;
                }
            })
            .withLocalEcho(false)
            .withTimeout(60)
            .addConversationAbandonedListener(event -> reopenGUI())
            .buildConversation(player)
            .begin();
    }

    private void toggleAutoStart() {
        boolean newValue = !arena.getConfig().isAutoStart();
        
        plugin.getFileManager().getArenas()
            .set("arenas." + arenaId + ".auto-start", newValue);
        plugin.getFileManager().getArenas().save();
        
        player.sendMessage(Text.createText(
            getConfigString("messages.auto-start-toggled", 
                "&aAuto-start {status}")
                .replace("{status}", newValue ? "enabled" : "disabled")).build(player));
        
        playSound(getConfigString("sounds.click", "UI_BUTTON_CLICK"));
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