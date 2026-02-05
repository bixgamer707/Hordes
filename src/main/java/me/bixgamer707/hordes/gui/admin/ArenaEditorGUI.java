package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;

/**
 * Arena editor GUI - 100% configurable
 * All sections, labels, and options from guis.yml
 */
public class ArenaEditorGUI extends BaseGUI {

    private final Arena arena;
    private final String arenaId;

    public ArenaEditorGUI(Hordes plugin, Player player, Arena arena) {
        super(plugin, player, "admin-arena-editor");
        this.arena = arena;
        this.arenaId = arena.getId();
    }

    @Override
    protected void buildDynamic() {
        // Update all dynamic data from arena
        updateAllSections();
    }

    /**
     * Updates all sections with current arena data
     */
    private void updateAllSections() {
        // Arena info (slot from config)
        updateItemLore("arena-info", new String[]{
                arenaId,
                arena.getConfig().getDisplayName(),
                arena.getState().name(),
                arena.getConfig().isEnabled() ?
                        getConfigString("text.enabled", "&a✔ Enabled") :
                        getConfigString("text.disabled", "&c✘ Disabled")
        });

        // Player settings
        updateItemLore("player-settings", new String[]{
                String.valueOf(arena.getConfig().getMinPlayers()),
                String.valueOf(arena.getConfig().getMaxPlayers()),
                String.valueOf(arena.getConfig().getCountdownTime())
        });

        // Wave settings
        updateItemLore("wave-settings", new String[]{
                String.valueOf(arena.getConfig().getTotalWaves()),
                String.valueOf(arena.getConfig().getWaveDelay()),
                arena.getConfig().getProgressionType().name()
        });

        // Mode settings
        updateItemLore("mode-settings", new String[]{
                arena.getConfig().getSurvivalMode().isEnabled() ?
                        getConfigString("text.survival", "Survival") :
                        getConfigString("text.arena", "Arena"),
                arena.getConfig().getDeathHandling().getAction().name(),
                arena.getConfig().getItemHandling().getDropMode().name()
        });

        // Spawn settings
        updateItemLore("spawn-settings", new String[]{
                arena.getConfig().getLobbySpawn() != null ?
                        getConfigString("text.spawn-set", "&a✔ Set") :
                        getConfigString("text.spawn-not-set", "&c✘ Not set"),
                arena.getConfig().getArenaSpawn() != null ?
                        getConfigString("text.spawn-set", "&a✔ Set") :
                        getConfigString("text.spawn-not-set", "&c✘ Not set"),
                arena.getConfig().getExitLocation() != null ?
                        getConfigString("text.spawn-set", "&a✔ Set") :
                        getConfigString("text.spawn-not-set", "&c✘ Not set")
        });

        // Reward settings
        updateItemLore("reward-settings", new String[]{
                arena.getConfig().getRewardConfig().getType().name(),
                String.format("%.2f", arena.getConfig().getRewardConfig().getMoney()),
                String.valueOf(arena.getConfig().getRewardConfig().getItems().size())
        });
    }

    @Override
    protected void handleCustomAction(String actionType, String actionValue, String itemId) {
        switch (actionType) {
            case "edit-basic":
                editBasicSettings();
                break;

            case "edit-players":
                // Future: PlayerSettingsGUI
                sendConfigMessage("admin.feature-coming-soon");
                break;

            case "edit-waves":
                // Future: WaveEditorGUI
                sendConfigMessage("admin.feature-coming-soon");
                break;

            case "edit-modes":
                // Future: ModeSettingsGUI
                sendConfigMessage("admin.feature-coming-soon");
                break;

            case "edit-spawns":
                new SpawnEditorGUI(plugin, player, arena).open();
                break;

            case "edit-rewards":
                // Future: RewardEditorGUI
                sendConfigMessage("admin.feature-coming-soon");
                break;

            case "edit-worldguard":
                editWorldGuardRegion();
                break;

            case "save-arena":
                saveArena();
                break;

            case "test-arena":
                testArena();
                break;

            case "toggle-enabled":
                toggleEnabled();
                break;
        }
    }

    /**
     * Edit basic settings via conversation
     */
    private void editBasicSettings() {
        close();

        // Send messages from config
        sendConfigMessage("admin.edit-basic-header");
        sendConfigMessage("admin.edit-basic-current", arena.getConfig().getDisplayName());
        sendConfigMessage("admin.edit-basic-prompt");

        ConversationFactory factory = new ConversationFactory(plugin)
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        return Text.createText(getConfigString("prompts.display-name", "Enter new display name:")).build();
                    }

                    @Override
                    public Prompt acceptInput(ConversationContext context, String input) {
                        if (input.equalsIgnoreCase("cancel")) {
                            sendConfigMessage("admin.edit-cancelled");
                            return Prompt.END_OF_CONVERSATION;
                        }

                        // Update display name
                        plugin.getFileManager().getFile("arenas.yml")
                                .set("arenas." + arenaId + ".display-name", input);
                        plugin.getFileManager().getArenas().save();

                        sendConfigMessage("admin.edit-basic-success", input);

                        return Prompt.END_OF_CONVERSATION;
                    }
                })
                .withLocalEcho(false)
                .withTimeout(60)
                .addConversationAbandonedListener(event -> {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        plugin.getArenaManager().loadArenas();
                        Arena reloaded = plugin.getArenaManager().getArena(arenaId);
                        if (reloaded != null) {
                            new ArenaEditorGUI(plugin, player, reloaded).open();
                        }
                    }, 1L);
                })
                .buildConversation(player);

        factory.begin();
    }

    /**
     * Edit WorldGuard region via conversation
     */
    private void editWorldGuardRegion() {
        close();

        sendConfigMessage("admin.edit-worldguard-header");
        String current = arena.getConfig().getWorldGuardRegion();
        sendConfigMessage("admin.edit-worldguard-current", current != null ? current : "None");
        sendConfigMessage("admin.edit-worldguard-prompt");

        ConversationFactory factory = new ConversationFactory(plugin)
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        return Text.createText(getConfigString("prompts.worldguard-region", "Enter region name:")).build();
                    }

                    @Override
                    public Prompt acceptInput(ConversationContext context, String input) {
                        if (input.equalsIgnoreCase("cancel")) {
                            sendConfigMessage("admin.edit-cancelled");
                            return Prompt.END_OF_CONVERSATION;
                        }

                        plugin.getFileManager().getFile("arenas.yml")
                                .set("arenas." + arenaId + ".worldguard-region", input);
                        plugin.getFileManager().getArenas().save();


                        sendConfigMessage("admin.edit-worldguard-success", input);

                        return Prompt.END_OF_CONVERSATION;
                    }
                })
                .withLocalEcho(false)
                .withTimeout(60)
                .addConversationAbandonedListener(event -> {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        plugin.getArenaManager().loadArenas();
                        Arena reloaded = plugin.getArenaManager().getArena(arenaId);
                        if (reloaded != null) {
                            new ArenaEditorGUI(plugin, player, reloaded).open();
                        }
                    }, 1L);
                })
                .buildConversation(player);

        factory.begin();
    }

    /**
     * Toggle enabled/disabled
     */
    private void toggleEnabled() {
        boolean newState = !arena.getConfig().isEnabled();
        arena.getConfig().setEnabled(newState);

        plugin.getFileManager().getFile("arenas.yml")
                .set("arenas." + arenaId + ".enabled", newState);
        plugin.getFileManager().getArenas().save();


        sendConfigMessage(newState ? "admin.arena-enabled" : "admin.arena-disabled", arenaId);
        playSound("click");
        refresh();
    }

    /**
     * Save arena
     */
    private void saveArena() {
        try {
            plugin.getFileManager().getArenas().save();

            plugin.getArenaManager().loadArenas();

            sendConfigMessage("admin.save-success");
            playSound("success");
            refresh();

        } catch (Exception e) {
            sendConfigMessage("admin.save-failed", e.getMessage());
            playSound("error");
        }
    }

    /**
     * Test arena
     */
    private void testArena() {
        close();

        boolean success = plugin.getArenaManager().joinArena(player, arenaId);

        if (!success) {
            sendConfigMessage("admin.test-failed");
        }
    }

    @Override
    protected void onBack() {
        new ArenaListGUI(plugin, player).open();
    }
}