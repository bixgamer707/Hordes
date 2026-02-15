package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.file.File;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
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
    protected void handleCustomAction(int slot, String actionType, String actionValue, String itemId) {
        if (actionType.equals("create-from-scratch")) {
            createFromScratch();
        } else if (actionType.equals("create-from-template")) {
            player.sendMessage(Text.createTextWithLang("admin.feature-coming-soon").build());
        }
    }

    /**
     * Create arena from scratch via chat input
     */
    private void createFromScratch() {
        File messages = plugin.getFileManager().getMessages();

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(messages.getString("Messages.prompts.arena-id", "&e&l❖ Enter Arena ID:"))
                .withValidator(InputValidators.arenaId())
                .withInvalidMessage(messages.getString("Messages.prompts.arena-id-invalid",
                        "&c&l✖ Invalid ID! Use only: a-z, 0-9, _ (underscore)"))
                .onComplete(input -> handleArenaIdInput(input))
                .onCancel(() -> new ArenaListGUI(plugin, player).open())
                .start();
    }

    /**
     * Handles arena ID input
     */
    private void handleArenaIdInput(String arenaId) {

        File messages = plugin.getFileManager().getMessages();
        // Check if already exists
        if (plugin.getArenaManager().getArena(arenaId) != null) {
            sendConfigMessage("Messages.admin.create-already-exists", messages, arenaId);
            new ArenaListGUI(plugin, player).open();
            return;
        }

        // Create arena configuration
        createArenaConfig(arenaId);

        // Send success message
        sendMessageListWithReplacements("Messages.admin.create", messages, arenaId);

        // Open editor
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Arena arena = plugin.getArenaManager().getArena(arenaId);
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
        File config = plugin.getFileManager().getConfig();
        // Get defaults from config
        int defaultMinPlayers = config.getInt("defaults.min-players", 1);
        int defaultMaxPlayers = config.getInt("defaults.max-players", 10);
        int defaultCountdown = config.getInt("defaults.countdown", 10);
        int defaultCooldown = config.getInt("defaults.cooldown", 300);
        int defaultWaves = config.getInt("defaults.total-waves", 5);
        int defaultWaveDelay = config.getInt("defaults.wave-delay", 10);
        boolean defaultAutoStart = config.getBoolean("defaults.auto-start", true);
        String defaultWaveProgression = config.getString("defaults.wave-progression", "AUTOMATIC");
        int defaultGlobalCooldown = config.getInt("defaults.global-cooldown", 300);

        // Create basic arena config
        File arenasFile = plugin.getFileManager().getArenas();
        arenasFile.set("arenas." + arenaId + ".enabled", false);
        arenasFile.set("arenas." + arenaId + ".display-name", arenaId);
        arenasFile.set("arenas." + arenaId + ".min-players", defaultMinPlayers);
        arenasFile.set("arenas." + arenaId + ".max-players", defaultMaxPlayers);
        arenasFile.set("arenas." + arenaId + ".countdown-time", defaultCountdown);
        arenasFile.set("arenas." + arenaId + ".cooldown", defaultCooldown);
        arenasFile.set("arenas." + arenaId + ".waves", defaultWaves);
        arenasFile.set("arenas." + arenaId + ".wave-delay", defaultWaveDelay);
        arenasFile.set("arenas." + arenaId + ".auto-start", defaultAutoStart);
        arenasFile.set("arenas." + arenaId + ".wave-progression", defaultWaveProgression);
        arenasFile.set("arenas." + arenaId + ".global-cooldown", defaultGlobalCooldown);

        plugin.getFileManager().getArenas().save();

        // Reload arenas
        plugin.getArenaManager().loadArenas();
    }

    @Override
    protected void onBack() {
        new ArenaListGUI(plugin, player).open();
    }
}