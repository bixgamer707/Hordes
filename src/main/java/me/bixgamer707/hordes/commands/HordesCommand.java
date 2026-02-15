package me.bixgamer707.hordes.commands;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.arena.ArenaManager;
import me.bixgamer707.hordes.gui.player.ArenaInfoGUI;
import me.bixgamer707.hordes.gui.player.ArenaSelectionGUI;
import me.bixgamer707.hordes.statistics.PlayerStatistics;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command handler for player commands
 * Handles: join, leave, list, info, stats
 *
 * Includes intelligent tab completion
 */
public class HordesCommand implements CommandExecutor, TabCompleter {

    private final Hordes plugin;
    private final ArenaManager arenaManager;
    
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "join", "leave", "list", "info", "stats", "top", "gui", "menu"
    );

    public HordesCommand(Hordes plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // No arguments - show help
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "join":
                return handleJoin(sender, args);
                
            case "leave":
                return handleLeave(sender, args);
                
            case "list":
                return handleList(sender, args);
                
            case "info":
                return handleInfo(sender, args);
                
            case "stats":
                return handleStats(sender, args);
                
            case "top":
                return handleTop(sender, args);
                
            case "gui":
            case "menu":
                return handleGUI(sender, args);
                
            case "help":
                sendHelp(sender);
                return true;
                
            default:
                sendMessage(sender, "commands.invalid-usage", "/hordes help");
                return true;
        }
    }

    /**
     * Handles /hordes join <arena>
     */
    private boolean handleJoin(CommandSender sender, String[] args) {
        // Must be a player
        if (!(sender instanceof Player)) {
            sendMessage(sender, "commands.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("hordes.join")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        // Check usage
        if (args.length < 2) {
            sendMessage(sender, "commands.join-usage");
            return true;
        }
        
        String arenaId = args[1];
        
        // Check if arena exists
        Arena arena = arenaManager.getArena(arenaId);
        
        if (arena == null) {
            sendMessage(sender, "commands.arena-not-found", arenaId);
            return true;
        }
        
        // Check specific arena permission
        if (!player.hasPermission("hordes.join.*") && 
            !player.hasPermission("hordes.join." + arenaId)) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        // Attempt to join
        boolean success = arenaManager.joinArena(player, arenaId);
        
        if (success) {
            sendMessage(sender, "commands.join-success");
        } else {
            sendMessage(sender, "commands.join-failed");
        }
        
        return true;
    }

    /**
     * Handles /hordes leave
     */
    private boolean handleLeave(CommandSender sender, String[] args) {
        // Must be a player
        if (!(sender instanceof Player)) {
            sendMessage(sender, "commands.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("hordes.leave")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        // Check if in arena
        Arena arena = arenaManager.getPlayerArena(player);
        
        if (arena == null) {
            sendMessage(sender, "commands.leave-not-in-arena");
            return true;
        }
        
        // Leave arena
        arenaManager.leaveArena(player, true);
        sendMessage(sender, "commands.leave-success");
        
        return true;
    }

    /**
     * Handles /hordes list
     */
    private boolean handleList(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("hordes.list")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        List<Arena> arenas = arenaManager.getEnabledArenas();
        
        if (arenas.isEmpty()) {
            sendMessage(sender, "commands.list-empty");
            return true;
        }
        
        // Send header
        sendMessage(sender, "commands.list-header");
        
        // List each arena
        for (Arena arena : arenas) {
            String status = arena.getState().getDisplayName();
            int current = arena.getPlayerCount();
            int max = arena.getConfig().getMaxPlayers();
            
            sendMessage(sender, "commands.list-format", 
                arena.getConfig().getDisplayName(),
                status,
                String.valueOf(current),
                String.valueOf(max)
            );
        }
        
        return true;
    }

    /**
     * Handles /hordes info <arena>
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("hordes.info")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        // Check usage
        if (args.length < 2) {
            sendMessage(sender, "commands.info-usage");
            return true;
        }
        
        String arenaId = args[1];
        Arena arena = arenaManager.getArena(arenaId);
        
        if (arena == null) {
            sendMessage(sender, "commands.arena-not-found", arenaId);
            return true;
        }
        
        // Display info using optimized list
        String mode = arena.getConfig().getSurvivalMode().isEnabled() ? "Survival" : "Arena";
        
        sendMessageListWithReplacements(sender, "commands.info",
            arenaId,                                        // {0}
            arena.getConfig().getDisplayName(),             // {1}
            arena.getState().getDisplayName(),              // {2}
            arena.getPlayerCount(),                         // {3}
            arena.getConfig().getMaxPlayers(),              // {4}
            arena.getConfig().getTotalWaves(),              // {5}
            mode,                                           // {6}
            arena.getConfig().getProgressionType().name()   // {7}
        );

        new ArenaInfoGUI(plugin, (Player) sender, arena).open();
        return true;
    }

    /**
     * Handles /hordes stats
     */
    private boolean handleStats(CommandSender sender, String[] args) {
        // Must be a player
        if (!(sender instanceof Player)) {
            sendMessage(sender, "commands.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("hordes.stats")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        // Check if statistics are enabled
        if (plugin.getStatisticsManager() == null || !plugin.getStatisticsManager().isEnabled()) {
            sendMessageList(sender, "commands.stats-disabled");
            return true;
        }
        
        // Get statistics
        PlayerStatistics stats = plugin.getStatisticsManager()
            .getStatistics(player.getUniqueId(), player.getName());
        
        if (stats == null) {
            sendMessage(sender, "commands.stats-no-data");
            return true;
        }
        
        // Format fastest time
        String fastestTime = stats.getFastestCompletion() > 0 
            ? formatTime(stats.getFastestCompletion()) 
            : "N/A";
        
        // Display statistics using list with placeholders
        sendMessageListWithReplacements(sender, "commands.stats",
            stats.getTotalCompletions(),                    // {0}
            stats.getTotalAttempts(),                       // {1}
            String.format("%.1f", stats.getWinRate()),      // {2}
            stats.getTotalKills(),                          // {3}
            stats.getTotalDeaths(),                         // {4}
            String.format("%.2f", stats.getKDRatio()),      // {5}
            stats.getHighestWave(),                         // {6}
            fastestTime,                                    // {7}
            formatTime(stats.getTotalPlaytime())            // {8}
        );
        
        return true;
    }
    
    /**
     * Sends a list of messages with placeholder replacements
     */
    private void sendMessageListWithReplacements(CommandSender sender, String path, Object... replacements) {
        List<String> messages = plugin.getFileManager().getMessages().getStringList("Messages." + path);
        
        if (messages.isEmpty()) {
            sendMessage(sender, path, replacements);
            return;
        }
        
        for (String message : messages) {
            // Replace placeholders
            for (int i = 0; i < replacements.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
            }
            
            if (sender instanceof Player) {
                sender.sendMessage(Text.createText(message).build((Player) sender));
            } else {
                sender.sendMessage(Text.createText(message).build());
            }
        }
    }
    
    /**
     * Handles /hordes top [category]
     */
    private boolean handleTop(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("hordes.top")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        // Default category
        String category = args.length >= 2 ? args[1].toLowerCase() : "completions";
        int limit = 10;
        
        switch (category) {
            case "completions":
            case "complete":
            case "wins":
                plugin.getLeaderboardManager().showTopCompletions(sender, limit);
                break;
                
            case "kills":
            case "kill":
                plugin.getLeaderboardManager().showTopKills(sender, limit);
                break;
                
            case "speed":
            case "fastest":
            case "time":
                plugin.getLeaderboardManager().showTopSpeed(sender, limit);
                break;
                
            case "winrate":
            case "wr":
                plugin.getLeaderboardManager().showTopWinRate(sender, limit);
                break;
                
            default:
                sendMessage(sender, "commands.top-invalid-category");
                break;
        }
        
        return true;
    }

    /**
     * Formats time in seconds to readable format
     */
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        
        long minutes = seconds / 60;
        long secs = seconds % 60;
        
        if (minutes < 60) {
            return minutes + "m " + secs + "s";
        }
        
        long hours = minutes / 60;
        minutes = minutes % 60;
        
        return hours + "h " + minutes + "m";
    }

    /**
     * Handles /hordes gui
     * Opens the arena selection GUI
     */
    private boolean handleGUI(CommandSender sender, String[] args) {
        // Must be a player
        if (!(sender instanceof Player)) {
            sendMessage(sender, "commands.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Open arena selection GUI
        new ArenaSelectionGUI(plugin, player).open();
        
        return true;
    }

    /**
     * Sends help message
     */
    private void sendHelp(CommandSender sender) {
        sendMessageList(sender, "commands.help");
    }
    
    /**
     * Sends a list of messages
     */
    private void sendMessageList(CommandSender sender, String path) {
        List<String> messages = plugin.getFileManager().getMessages().getStringList("Messages." + path);
        
        if (messages.isEmpty()) {
            sendMessage(sender, path);
            return;
        }
        
        for (String message : messages) {
            if (sender instanceof Player) {
                sender.sendMessage(Text.createText(message).build((Player) sender));
            } else {
                sender.sendMessage(Text.createText(message).build());
            }
        }
    }

    /**
     * Sends a formatted message
     */
    private void sendMessage(CommandSender sender, String path, Object... replacements) {
        String message = plugin.getFileManager().getMessages().getString("Messages." + path, path);
        
        // Replace placeholders
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
        }
        
        // Apply colors and placeholders
        if (sender instanceof Player) {
            sender.sendMessage(Text.createText(message).build((Player) sender));
        } else {
            sender.sendMessage(Text.createText(message).build());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // First argument - subcommands
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            
            for (String subCmd : SUBCOMMANDS) {
                if (subCmd.startsWith(input)) {
                    // Check permission
                    if (sender.hasPermission("hordes." + subCmd) || 
                        sender.hasPermission("hordes.use")) {
                        completions.add(subCmd);
                    }
                }
            }
            
            return completions;
        }
        
        // Second argument - depends on subcommand
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            
            switch (subCommand) {
                case "join":
                    // Complete arena names
                    return getArenaCompletions(sender, input, false);
                    
                case "info":
                    // Complete all arena names (including disabled)
                    return getArenaCompletions(sender, input, true);
                    
                default:
                    return completions;
            }
        }
        
        return completions;
    }

    /**
     * Gets arena name completions
     * 
     * @param sender Command sender
     * @param input Current input
     * @param includeDisabled Whether to include disabled arenas
     * @return List of matching arena names
     */
    private List<String> getArenaCompletions(CommandSender sender, String input, boolean includeDisabled) {
        List<Arena> arenas = includeDisabled ? 
            new ArrayList<>(arenaManager.getArenas().values()) :
            arenaManager.getEnabledArenas();
        
        return arenas.stream()
            .filter(arena -> {
                // Check if sender has permission for this arena
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    return player.hasPermission("hordes.join.*") || 
                           player.hasPermission("hordes.join." + arena.getId());
                }
                return true;
            })
            .map(Arena::getId)
            .filter(id -> id.toLowerCase().startsWith(input))
            .sorted()
            .collect(Collectors.toList());
    }
}
