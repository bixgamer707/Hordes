package me.bixgamer707.hordes.commands;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.arena.ArenaManager;
import me.bixgamer707.hordes.arena.ArenaState;
import me.bixgamer707.hordes.config.SpawnConfigManager;
import me.bixgamer707.hordes.gui.admin.AdminMainGUI;
import me.bixgamer707.hordes.gui.admin.ArenaCreateGUI;
import me.bixgamer707.hordes.gui.admin.ArenaListGUI;
import me.bixgamer707.hordes.gui.player.LeaderboardGUI;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Location;
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
 * Admin command handler
 * Handles: reload, create, delete, setspawn, forcestart, forcestop, tp, debug

 * Includes intelligent tab completion with context awareness
 */
public class HordesAdminCommand implements CommandExecutor, TabCompleter {

    private final Hordes plugin;
    private final ArenaManager arenaManager;
    
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "reload", "create", "delete", "setspawn", 
        "forcestart", "forcestop", "tp", "debug", "arenas", "config"
    );
    
    private static final List<String> SPAWN_TYPES = Arrays.asList(
        "lobby", "arena", "exit"
    );

    public HordesAdminCommand(Hordes plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check admin permission
        if (!sender.hasPermission("hordes.admin")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        // No arguments - show help
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReload(sender, args);
                
            case "create":
                return handleCreate(sender, args);
                
            case "delete":
                return handleDelete(sender, args);
                
            case "setspawn":
                return handleSetSpawn(sender, args);
                
            case "forcestart":
                return handleForceStart(sender, args);
                
            case "forcestop":
                return handleForceStop(sender, args);
                
            case "tp":
                return handleTeleport(sender, args);
                
            case "debug":
                return handleDebug(sender, args);
                
            case "help":
                sendHelp(sender);
                return true;
            case "arenas":
                handleArenas(sender);
                return true;
            case "top":
                handleTop(sender);
                return true;
            case "config":
                handleConfig(sender);
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void handleConfig(CommandSender sender) {
        if (!sender.hasPermission("hordes.admin.config")) {
            sendMessage(sender, "commands.no-permission");
            return;
        }

        if (sender instanceof Player) {
            new AdminMainGUI(plugin, (Player) sender).open();
        } else {
            sendMessage(sender, "commands.player-only");
        }
    }

    private void handleTop(CommandSender sender) {
        if (!sender.hasPermission("hordes.admin.top")) {
            sendMessage(sender, "commands.no-permission");
            return;
        }

        if (arenaManager.getArenas().isEmpty()) {
            sendMessage(sender, "commands.admin.arenas-none");
            return;
        }

        new LeaderboardGUI(plugin, (Player) sender).open();
    }

    private void handleArenas(CommandSender sender) {
        if (!sender.hasPermission("hordes.admin.arenas")) {
            sendMessage(sender, "commands.no-permission");
            return;
        }

        if (arenaManager.getArenas().isEmpty()) {
            sendMessage(sender, "admin.arenas-none");
            return;
        }

        ArenaListGUI gui = new ArenaListGUI(plugin, sender instanceof Player ? (Player) sender : null);

        gui.open();
    }

    /**
     * Handles /hordesadmin reload
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hordes.admin.reload")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        try {
            plugin.reload();
            sendMessage(sender, "admin.reload-success");
            return true;
        } catch (Exception e) {
            sendMessage(sender, "admin.reload-failed");
            sendMessage(sender, "errors.config-error", e.getMessage());
            if (plugin.getFileManager().getFile("config.yml").getBoolean("debug-mode", false)) {
                e.printStackTrace();
            }
            return true;
        }
    }

    /**
     * Handles /hordesadmin create <arena>
     */
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hordes.admin.create")) {
            sendMessage(sender, "commands.no-permission");
            return false;
        }

        new ArenaCreateGUI(plugin, sender instanceof Player ? (Player) sender : null).open();
        return true;
    }

    /**
     * Handles /hordesadmin delete <arena>
     */
    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hordes.admin.delete")) {
            sendMessage(sender, "commands.no-permission");
            return false;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "admin.delete-usage");
            return true;
        }
        
        String arenaId = args[1];
        Arena arena = arenaManager.getArena(arenaId);
        
        if (arena == null) {
            sendMessage(sender, "commands.arena-not-found", arenaId);
            return true;
        }
        
        // Stop arena if active
        if (arena.getState() != ArenaState.WAITING) {
            arena.endArena(false);
        }
        
        sendMessage(sender, "admin.delete-stopped");
        sendMessage(sender, "admin.delete-instruction");
        
        return true;
    }

    /**
     * Handles /hordesadmin setspawn <arena> <type>
     */
    private boolean handleSetSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hordes.admin.setspawn")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sendMessage(sender, "commands.player-only");
            return true;
        }
        
        if (args.length < 3) {
            sendMessage(sender, "admin.setspawn-usage");
            return true;
        }
        
        Player player = (Player) sender;
        String arenaId = args[1];
        String spawnType = args[2].toLowerCase();
        
        Arena arena = arenaManager.getArena(arenaId);
        
        if (arena == null) {
            sendMessage(sender, "admin.setspawn-arena-not-found", arenaId);
            return true;
        }
        
        if (!SPAWN_TYPES.contains(spawnType)) {
            sendMessage(sender, "admin.setspawn-invalid-type");
            return true;
        }
        
        // Get player location
        Location loc = player.getLocation();
        
        // Save to configuration
        SpawnConfigManager spawnManager = new SpawnConfigManager(plugin);
        boolean success = spawnManager.setSpawn(arenaId, spawnType, loc);
        
        if (success) {
            // Show success message using optimized list
            sendMessageListWithReplacements(sender, "admin.setspawn",
                arenaId,                                    // {0}
                spawnType,                                  // {1}
                loc.getWorld().getName(),                   // {2}
                loc.getBlockX(),                            // {3}
                loc.getBlockY(),                            // {4}
                loc.getBlockZ(),                            // {5}
                String.format("%.1f", loc.getYaw()),        // {6}
                String.format("%.1f", loc.getPitch())       // {7}
            );
        } else {
            sendMessage(sender, "admin.setspawn-failed");
            sendMessage(sender, "admin.setspawn-check-console");
        }
        
        return true;
    }

    /**
     * Handles /hordesadmin forcestart <arena>
     */
    private boolean handleForceStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hordes.admin.forcestart")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "commands.admin.forcestart-usage");
            return true;
        }
        
        String arenaId = args[1];
        Arena arena = arenaManager.getArena(arenaId);
        
        if (arena == null) {
            sendMessage(sender, "commands.arena-not-found", arenaId);
            return true;
        }
        
        // Check if can start
        if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.STARTING) {
            sendMessage(sender, "admin.forcestart-not-waiting");
            return true;
        }
        
        if (arena.getPlayerCount() == 0) {
            sendMessage(sender, "admin.forcestart-no-players");
            return true;
        }
        
        sendMessage(sender, "admin.forcestart-starting");
        sendMessage(sender, "admin.forcestart-note");
        sendMessage(sender, "admin.forcestart-players", arena.getPlayerCount());
        
        return true;
    }

    /**
     * Handles /hordesadmin forcestop <arena>
     */
    private boolean handleForceStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hordes.admin.forcestop")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "admin.forcestop-usage");
            return true;
        }
        
        String arenaId = args[1];
        Arena arena = arenaManager.getArena(arenaId);
        
        if (arena == null) {
            sendMessage(sender, "commands.arena-not-found", arenaId);
            return true;
        }
        
        // Force stop
        arena.endArena(false);
        sendMessage(sender, "admin.forcestop", arenaId);
        
        return true;
    }

    /**
     * Handles /hordesadmin tp <arena>
     */
    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hordes.admin.tp")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sendMessage(sender, "commands.player-only");
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "admin.tp-usage");
            return true;
        }
        
        Player player = (Player) sender;
        String arenaId = args[1];
        Arena arena = arenaManager.getArena(arenaId);
        
        if (arena == null) {
            sendMessage(sender, "commands.arena-not-found", arenaId);
            return true;
        }
        
        // Teleport to arena spawn
        player.teleport(arena.getConfig().getArenaSpawn());
        sendMessage(sender, "admin.tp-success", arenaId);
        
        return true;
    }

    /**
     * Handles /hordesadmin debug
     */
    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hordes.admin.debug")) {
            sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        // Get arena counts
        int totalArenas = arenaManager.getArenas().size();
        int activeArenas = 0;

        for (Arena arena : arenaManager.getArenas().values()) {
            if (arena.getState() != ArenaState.ACTIVE) {
                activeArenas++;
            }
        }
        
        // Display debug info using optimized list
        sendMessageListWithReplacements(sender, "admin.debug",
            totalArenas,                                                // {0}
            activeArenas,                                               // {1}
            plugin.getCooldownManager().getActiveCooldownCount(),      // {2}
            plugin.getRewardManager().isEconomyEnabled(),              // {3}
            plugin.getMythicMobsIntegration().isEnabled()              // {4}
        );
        
        return true;
    }

    /**
     * Sends help message
     */
    private void sendHelp(CommandSender sender) {
        sendMessageList(sender, "admin.help");
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
                    if (sender.hasPermission("hordes.admin." + subCmd) || 
                        sender.hasPermission("hordes.admin")) {
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
                case "delete":
                case "forcestart":
                case "forcestop":
                case "tp":
                case "setspawn":
                    // Complete with all arena names
                    return arenaManager.getArenaIds().stream()
                        .filter(id -> id.toLowerCase().startsWith(input))
                        .sorted()
                        .collect(Collectors.toList());
                    
                default:
                    return completions;
            }
        }
        
        // Third argument - spawn types for setspawn
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("setspawn")) {
                String input = args[2].toLowerCase();
                
                return SPAWN_TYPES.stream()
                    .filter(type -> type.startsWith(input))
                    .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
}
