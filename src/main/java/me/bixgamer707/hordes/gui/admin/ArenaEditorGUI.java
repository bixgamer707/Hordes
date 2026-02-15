package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.file.File;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
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
                        Text.createTextWithLang("guis."+guiId+".text.enabled", guiConfig).build() :
                        Text.createTextWithLang("guis."+guiId+".text.disabled", guiConfig).build()
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
                        Text.createTextWithLang("guis."+guiId+".text.survival").build() :
                        Text.createTextWithLang("guis."+guiId+".text.arena").build(),
                arena.getConfig().getDeathHandling().getAction().name(),
                arena.getConfig().getItemHandling().getDropMode().name()
        });

        // Spawn settings
        updateItemLore("spawn-settings", new String[]{
                arena.getConfig().getLobbySpawn() != null ?
                        Text.createTextWithLang("guis."+guiId+".text.spawn-set").build() :
                        Text.createTextWithLang("guis."+guiId+".text.spawn-no-set").build(),
                arena.getConfig().getArenaSpawn() != null ?
                        Text.createTextWithLang("guis."+guiId+".text.spawn-set").build() :
                        Text.createTextWithLang("guis."+guiId+".text.spawn-no-set").build(),
                arena.getConfig().getExitLocation() != null ?
                        Text.createTextWithLang("guis."+guiId+".text.spawn-set").build() :
                        Text.createTextWithLang("guis."+guiId+".text.spawn-no-set").build()
        });

        // Reward settings
        updateItemLore("reward-settings", new String[]{
                arena.getConfig().getRewardConfig().getType().name(),
                String.format("%.2f", arena.getConfig().getRewardConfig().getMoney()),
                String.valueOf(arena.getConfig().getRewardConfig().getItems().size())
        });
    }

    @Override
    protected void handleCustomAction(int slot, String actionType, String actionValue, String itemId) {
        switch (actionType) {
            case "edit-basic":
                editBasicSettings();
                break;

            case "edit-players":
                player.sendMessage(Text.createTextWithLang("admin.feature-coming-soon").build(player));
                break;

            case "edit-waves":
                player.sendMessage(Text.createTextWithLang("admin.feature-coming-soon").build(player));

                break;

            case "edit-modes":
                player.sendMessage(Text.createTextWithLang("admin.feature-coming-soon").build(player));
                break;

            case "edit-spawns":
                new SpawnEditorGUI(plugin, player, arena).open();
                break;

            case "edit-rewards":
                new RewardEditorGUI(plugin, player, arena).open();
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
        File messages = plugin.getFileManager().getMessages();
        plugin.getChatInputManager().requestInput(player)
                .withPrompt(messages.getString("Messages.prompts.display-name",
                        "&e&l❖ Enter new display name:"))
                .withValidator(InputValidators.displayName())
                .withInvalidMessage(messages.getString("Messages.prompts.display-name-invalid",
                        "&c&l✖ Invalid name! Must be 3-32 characters"))
                .onComplete(input -> handleDisplayNameInput(input))
                .onCancel(() -> reopenEditor())
                .start();
    }

    /**
     * Handles display name input
     */
    private void handleDisplayNameInput(String displayName) {
        // Update display name
        File messages = plugin.getFileManager().getMessages();
        plugin.getFileManager().getFile("arenas.yml")
                .set("arenas." + arenaId + ".display-name", displayName);
        plugin.getFileManager().getArenas().save();

        sendConfigMessage("Messages.admin.edit-basic-success", messages, displayName);
        playSound("success");

        // Reload and reopen
        reloadAndReopen();
    }

    /**
     * Edit WorldGuard region via chat input
     */
    private void editWorldGuardRegion() {

        File messages = plugin.getFileManager().getMessages();
        plugin.getChatInputManager().requestInput(player)
                .withPrompt(messages.getString("Messages.prompts.worldguard-region",
                        "&e&l❖ Enter WorldGuard region name:"))
                .withValidator(InputValidators.regionName())
                .withInvalidMessage(messages.getString("Messages.prompts.region-invalid",
                        "&c&l✖ Invalid region name! Max 64 characters"))
                .onComplete(input -> handleWorldGuardRegionInput(input))
                .onCancel(() -> reopenEditor())
                .start();
    }

    /**
     * Handles WorldGuard region input
     */
    private void handleWorldGuardRegionInput(String regionName) {
        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".worldguard-region", regionName);
        plugin.getFileManager().getArenas().save();

        sendConfigMessage("Messages.admin.edit-worldguard-success", plugin.getFileManager().getMessages(), regionName);
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

        plugin.getFileManager().getArenas()
                .set("arenas." + arenaId + ".enabled", newState);
        plugin.getFileManager().getArenas().save();

        sendConfigMessage(newState ? "admin.arena-enabled" : "admin.arena-disabled", plugin.getFileManager().getMessages(), arenaId);
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

            sendConfigMessage("Messages.admin.save-success", plugin.getFileManager().getMessages());
            playSound("success");
            refresh();

        } catch (Exception e) {
            sendConfigMessage("Messages.admin.save-failed", plugin.getFileManager().getMessages(), e.getMessage());
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
            sendConfigMessage("Messages.admin.test-failed", plugin.getFileManager().getMessages());
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