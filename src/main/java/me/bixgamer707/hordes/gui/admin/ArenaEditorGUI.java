package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.utils.InputValidators;
import org.bukkit.entity.Player;

/**
 * Arena editor GUI - 100% configurable
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
        updateAllSections();
    }

    /**
     * Updates all sections with current arena data
     */
    private void updateAllSections() {
        // Arena info
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
                sendConfigMessage("admin.feature-coming-soon");
                break;

            case "edit-waves":
                sendConfigMessage("admin.feature-coming-soon");
                break;

            case "edit-modes":
                sendConfigMessage("admin.feature-coming-soon");
                break;

            case "edit-spawns":
                new SpawnEditorGUI(plugin, player, arena).open();
                break;

            case "edit-rewards":
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
     * Edit basic settings via chat input
     */
    private void editBasicSettings() {
        sendConfigMessage("admin.edit-basic-header");
        sendConfigMessage("admin.edit-basic-current", arena.getConfig().getDisplayName());
        sendConfigMessage("admin.edit-basic-prompt");

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(getConfigString("prompts.display-name",
                        "&e&l❖ Enter new display name:"))
                .withValidator(InputValidators.displayName())
                .withInvalidMessage(getConfigString("prompts.display-name-invalid",
                        "&c&l✖ Invalid name! Must be 3-32 characters"))
                .withTimeout(getConfigInt("prompts.timeout", 60))
                .onComplete(input -> handleDisplayNameInput(input))
                .onCancel(() -> reopenEditor())
                .start();
    }

    /**
     * Handles display name input
     */
    private void handleDisplayNameInput(String displayName) {
        // Update display name
        plugin.getFileManager().getFile("arenas.yml")
                .set("arenas." + arenaId + ".display-name", displayName);
        plugin.getFileManager().getArenas().save();

        sendConfigMessage("admin.edit-basic-success", displayName);
        playSound("success");

        // Reload and reopen
        reloadAndReopen();
    }

    /**
     * Edit WorldGuard region via chat input
     */
    private void editWorldGuardRegion() {
        sendConfigMessage("admin.edit-worldguard-header");

        String current = arena.getConfig().getWorldGuardRegion();
        sendConfigMessage("admin.edit-worldguard-current", current != null ? current : "None");
        sendConfigMessage("admin.edit-worldguard-prompt");

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(getConfigString("prompts.worldguard-region",
                        "&e&l❖ Enter WorldGuard region name:"))
                .withValidator(InputValidators.regionName())
                .withInvalidMessage(getConfigString("prompts.region-invalid",
                        "&c&l✖ Invalid region name! Max 64 characters"))
                .withTimeout(getConfigInt("prompts.timeout", 60))
                .onComplete(input -> handleWorldGuardRegionInput(input))
                .onCancel(() -> reopenEditor())
                .start();
    }

    /**
     * Handles WorldGuard region input
     */
    private void handleWorldGuardRegionInput(String regionName) {
        plugin.getFileManager().getFile("arenas.yml")
                .set("arenas." + arenaId + ".worldguard-region", regionName);
        plugin.getFileManager().getArenas().save();

        sendConfigMessage("admin.edit-worldguard-success", regionName);
        playSound("success");

        // Reload and reopen
        reloadAndReopen();
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

        reloadAndReopen();
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

    /**
     * Reloads arena manager and reopens editor
     */
    private void reloadAndReopen() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getArenaManager().loadArenas();
            Arena reloaded = plugin.getArenaManager().getArena(arenaId);

            if (reloaded != null) {
                new ArenaEditorGUI(plugin, player, reloaded).open();
            } else {
                new ArenaListGUI(plugin, player).open();
            }
        }, 1L);
    }

    /**
     * Reopens editor without reload
     */
    private void reopenEditor() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            new ArenaEditorGUI(plugin, player, arena).open();
        }, 1L);
    }

    @Override
    protected void onBack() {
        new ArenaListGUI(plugin, player).open();
    }
}