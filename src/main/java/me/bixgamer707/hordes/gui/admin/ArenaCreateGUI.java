package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.utils.InputValidators;
import org.bukkit.entity.Player;

/**
 * Arena creation GUI - 100% configurable
 */
public class ArenaCreateGUI extends BaseGUI {

    public ArenaCreateGUI(Hordes plugin, Player player) {
        super(plugin, player, "admin-arena-create");
    }

    @Override
    protected void buildDynamic() {
        // Static items from config
    }

    @Override
    protected void handleCustomAction(String actionType, String actionValue, String itemId) {
        if (actionType.equals("create-from-scratch")) {
            createFromScratch();
        } else if (actionType.equals("create-from-template")) {
            sendConfigMessage("admin.feature-coming-soon");
        }
    }

    /**
     * Create arena from scratch via chat input
     */
    private void createFromScratch() {
        sendConfigMessage("admin.create-header");
        sendConfigMessage("admin.create-prompt");

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(getConfigString("prompts.arena-id", "&e&l❖ Enter Arena ID:"))
                .withValidator(InputValidators.arenaId())
                .withInvalidMessage(getConfigString("prompts.arena-id-invalid",
                        "&c&l✖ Invalid ID! Use only: a-z, 0-9, _ (underscore)"))
                .withTimeout(getConfigInt("prompts.timeout", 60))
                .withTimeoutMessage(getConfigString("prompts.timeout-message",
                        "&c&l⏱ Input timed out"))
                .onComplete(input -> handleArenaIdInput(input))
                .onCancel(() -> new ArenaListGUI(plugin, player).open())
                .start();
    }

    /**
     * Handles arena ID input
     */
    private void handleArenaIdInput(String arenaId) {
        // Check if already exists
        if (plugin.getArenaManager().getArena(arenaId) != null) {
            sendConfigMessage("admin.create-already-exists", arenaId);
            new ArenaListGUI(plugin, player).open();
            return;
        }

        // Create arena configuration
        createArenaConfig(arenaId);

        // Send success message
        sendMessageListWithReplacements("admin.create", arenaId);

        // Open editor
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            var arena = plugin.getArenaManager().getArena(arenaId);
            if (arena != null) {
                new ArenaEditorGUI(plugin, player, arena).open();
            } else {
                new ArenaListGUI(plugin, player).open();
            }
        }, 1L);
    }

    /**
     * Creates default arena configuration
     */
    private void createArenaConfig(String arenaId) {
        // Get defaults from config
        int defaultMinPlayers = getConfigInt("defaults.min-players", 1);
        int defaultMaxPlayers = getConfigInt("defaults.max-players", 10);
        int defaultCountdown = getConfigInt("defaults.countdown", 10);
        int defaultWaves = getConfigInt("defaults.total-waves", 5);

        // Create basic arena config
        var arenasFile = plugin.getFileManager().getFile("arenas.yml");
        arenasFile.set("arenas." + arenaId + ".enabled", false);
        arenasFile.set("arenas." + arenaId + ".display-name", arenaId);
        arenasFile.set("arenas." + arenaId + ".min-players", defaultMinPlayers);
        arenasFile.set("arenas." + arenaId + ".max-players", defaultMaxPlayers);
        arenasFile.set("arenas." + arenaId + ".countdown-time", defaultCountdown);
        arenasFile.set("arenas." + arenaId + ".waves", defaultWaves);
        plugin.getFileManager().getArenas().save();

        // Reload arenas
        plugin.getArenaManager().loadArenas();
    }

    @Override
    protected void onBack() {
        new ArenaListGUI(plugin, player).open();
    }
}