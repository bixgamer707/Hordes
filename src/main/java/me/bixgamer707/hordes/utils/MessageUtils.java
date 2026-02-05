package me.bixgamer707.hordes.utils;

import me.bixgamer707.hordes.text.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Helper class for sending messages using the Text system
 * Simplifies the usage of Text.java throughout the plugin
 */
public class MessageUtils {

    /**
     * Sends a message to a player with placeholders
     * 
     * @param player Player to send message to
     * @param path Message path in messages file
     * @param replacements Placeholder replacements for {0}, {1}, etc.
     */
    public static void sendMessage(Player player, String path, Object... replacements) {
        String message = getMessage(path, replacements);
        player.sendMessage(Text.createText(message).build(player));
    }

    /**
     * Sends a message to a CommandSender (player or console)
     * 
     * @param sender CommandSender to send message to
     * @param path Message path in messages file
     * @param replacements Placeholder replacements
     */
    public static void sendMessage(CommandSender sender, String path, Object... replacements) {
        String message = getMessage(path, replacements);
        
        if (sender instanceof Player) {
            sender.sendMessage(Text.createText(message).build((Player) sender));
        } else {
            sender.sendMessage(Text.createText(message).build());
        }
    }

    /**
     * Gets a formatted message without sending
     * 
     * @param path Message path in messages file
     * @param replacements Placeholder replacements
     * @return Formatted and colorized message
     */
    public static String getMessage(String path, Object... replacements) {
        String message = Text.getMessages().getString("Messages." + path, path);
        
        // Replace {0}, {1}, {2}...
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
        }
        
        return message;
    }

    /**
     * Gets a formatted message with colors applied
     * 
     * @param path Message path
     * @param replacements Placeholder replacements
     * @return Fully processed message with colors
     */
    public static String getColoredMessage(String path, Object... replacements) {
        String message = getMessage(path, replacements);
        return Text.createText(message).build();
    }

    /**
     * Broadcasts a message to multiple players
     * 
     * @param players Array of players
     * @param path Message path
     * @param replacements Placeholder replacements
     */
    public static void broadcast(Iterable<Player> players, String path, Object... replacements) {
        String message = getMessage(path, replacements);
        
        for (Player player : players) {
            player.sendMessage(Text.createText(message).build(player));
        }
    }
}
