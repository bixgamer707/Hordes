package me.bixgamer707.hordes.leaderboard;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.statistics.PlayerStatistics;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Manages leaderboards for different statistics
 * Displays top players in various categories
 */
public class LeaderboardManager {

    private final Hordes plugin;

    public LeaderboardManager(Hordes plugin) {
        this.plugin = plugin;
    }

    /**
     * Displays top players by completions
     */
    public void showTopCompletions(CommandSender sender, int limit) {
        if (!isEnabled()) {
            sendMessage(sender, "leaderboard.disabled");
            return;
        }

        List<PlayerStatistics> top = plugin.getStatisticsManager()
            .getTopByCompletions(limit);

        displayLeaderboard(sender, getText("leaderboard.completions"), top, limit, 
            stats -> String.valueOf(stats.getTotalCompletions()));
    }

    /**
     * Displays top players by kills
     */
    public void showTopKills(CommandSender sender, int limit) {
        if (!isEnabled()) {
            sendMessage(sender, "leaderboard.disabled");
            return;
        }

        List<PlayerStatistics> top = plugin.getStatisticsManager()
            .getTopByKills(limit);

        displayLeaderboard(sender, getText("leaderboard.kills"), top, limit, 
            stats -> String.valueOf(stats.getTotalKills()));
    }

    /**
     * Displays top players by fastest completion
     */
    public void showTopSpeed(CommandSender sender, int limit) {
        if (!isEnabled()) {
            sendMessage(sender, "leaderboard.disabled");
            return;
        }

        List<PlayerStatistics> top = plugin.getStatisticsManager()
            .getTopBySpeed(limit);

        displayLeaderboard(sender, getText("leaderboard.speed"), top, limit, 
            stats -> formatTime(stats.getFastestCompletion()));
    }

    /**
     * Displays top players by win rate
     */
    public void showTopWinRate(CommandSender sender, int limit) {
        if (!isEnabled()) {
            sendMessage(sender, "leaderboard.disabled");
            return;
        }

        List<PlayerStatistics> top = plugin.getStatisticsManager()
            .getTopByCompletions(limit);
        
        // Sort by win rate
        top.sort((a, b) -> Double.compare(b.getWinRate(), a.getWinRate()));

        displayLeaderboard(sender, getText("leaderboard.winrate"), top, limit, 
            stats -> String.format("%.1f%%", stats.getWinRate()));
    }

    /**
     * Generic leaderboard display
     */
    private void displayLeaderboard(CommandSender sender, String category, 
                                   List<PlayerStatistics> players, int limit,
                                   StatFormatter formatter) {
        sendMessageListWithReplacements(sender, "commands.top-header", limit, category);

        if (players.isEmpty()) {
            sendMessage(sender, "commands.top-no-data");
        } else {
            int position = 1;
            for (PlayerStatistics stats : players) {
                String medal = getMedal(position);
                String value = formatter.format(stats);
                
                sendMessage(sender, "leaderboard.position", medal, position, stats.getPlayerName(), value);
                
                position++;
                if (position > limit) break;
            }
        }

        sendMessage(sender, "commands.top-footer");
    }
    
    /**
     * Sends a message using Text system
     */
    private void sendMessage(CommandSender sender, String path, Object... args) {
        String message = Text.getMessages().getString("Messages." + path, path);
        
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }
        
        if (sender instanceof org.bukkit.entity.Player) {
            sender.sendMessage(Text.createText(message).build((org.bukkit.entity.Player) sender));
        } else {
            sender.sendMessage(Text.createText(message).build());
        }
    }
    
    /**
     * Gets text from messages file
     */
    private String getText(String path) {
        return Text.getMessages().getString("Messages." + path, path);
    }

    /**
     * Gets medal emoji for position
     */
    private String getMedal(int position) {
        switch (position) {
            case 1: return "§6🥇";
            case 2: return "§7🥈";
            case 3: return "§c🥉";
            default: return "§7 ";
        }
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
     * Checks if statistics are enabled
     */
    private boolean isEnabled() {
        return plugin.getStatisticsManager() != null && 
               plugin.getStatisticsManager().isEnabled();
    }
    
    /**
     * Sends a list of messages with placeholder replacements
     */
    private void sendMessageListWithReplacements(CommandSender sender, String path, Object... replacements) {
        List<String> messages = Text.getMessages().getStringList("Messages." + path);
        
        if (messages.isEmpty()) {
            sendMessage(sender, path, replacements);
            return;
        }
        
        for (String message : messages) {
            // Replace placeholders
            for (int i = 0; i < replacements.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
            }
            
            sender.sendMessage(Text.createText(message).build());
        }
    }

    /**
     * Functional interface for stat formatting
     */
    @FunctionalInterface
    private interface StatFormatter {
        String format(PlayerStatistics stats);
    }
}
