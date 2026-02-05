package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.conversations.*;
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
            // Future: templates
            sendConfigMessage("admin.feature-coming-soon");
        }
    }

    /**
     * Create arena from scratch via conversation
     */
    private void createFromScratch() {
        close();

        sendConfigMessage("admin.create-header");
        sendConfigMessage("admin.create-prompt");

        ConversationFactory factory = new ConversationFactory(plugin)
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        return Text.createText(getConfigString("prompts.arena-id", "Enter arena ID:")).build();
                    }

                    @Override
                    public Prompt acceptInput(ConversationContext context, String input) {
                        // Validate ID
                        if (!input.matches("[a-z0-9_]+")) {
                            sendConfigMessage("admin.create-invalid-id");
                            return this;
                        }

                        // Check if exists
                        if (plugin.getArenaManager().getArena(input) != null) {
                            sendConfigMessage("admin.create-already-exists", input);
                            return this;
                        }

                        // Get default values from config
                        int defaultMinPlayers = getConfigInt("defaults.min-players", 1);
                        int defaultMaxPlayers = getConfigInt("defaults.max-players", 10);
                        int defaultCountdown = getConfigInt("defaults.countdown", 10);
                        int defaultWaves = getConfigInt("defaults.total-waves", 5);

                        // Create basic arena config
                        plugin.getFileManager().getFile("arenas.yml").set("arenas." + input + ".enabled", false);
                        plugin.getFileManager().getFile("arenas.yml").set("arenas." + input + ".display-name", input);
                        plugin.getFileManager().getFile("arenas.yml").set("arenas." + input + ".min-players", defaultMinPlayers);
                        plugin.getFileManager().getFile("arenas.yml").set("arenas." + input + ".max-players", defaultMaxPlayers);
                        plugin.getFileManager().getFile("arenas.yml").set("arenas." + input + ".countdown-time", defaultCountdown);
                        plugin.getFileManager().getFile("arenas.yml").set("arenas." + input + ".total-waves", defaultWaves);
                        plugin.getFileManager().getArenas().save();


                        // Reload arenas
                        plugin.getArenaManager().loadArenas();

                        sendConfigMessage("admin.create-success", input);

                        context.setSessionData("arenaId", input);

                        return Prompt.END_OF_CONVERSATION;
                    }
                })
                .withLocalEcho(false)
                .withTimeout(60)
                .addConversationAbandonedListener(event -> {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        String arenaId = (String) event.getContext().getSessionData("arenaId");
                        if (arenaId != null) {
                            var arena = plugin.getArenaManager().getArena(arenaId);
                            if (arena != null) {
                                new ArenaEditorGUI(plugin, player, arena).open();
                                return;
                            }
                        }
                        new ArenaListGUI(plugin, player).open();
                    }, 1L);
                })
                .buildConversation(player);

        factory.begin();
    }

    @Override
    protected void onBack() {
        new ArenaListGUI(plugin, player).open();
    }
}