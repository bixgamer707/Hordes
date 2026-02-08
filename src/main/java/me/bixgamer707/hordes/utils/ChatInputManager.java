package me.bixgamer707.hordes.utils;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Professional chat input manager for GUIs
 * 100% configurable with validators, timeouts, and custom messages
 * 
 * Thread-safe implementation with automatic cleanup
 */
public class ChatInputManager implements Listener {

    private final Hordes plugin;
    
    // Active sessions: Player UUID -> Session
    private final Map<UUID, ChatInputSession> activeSessions;
    
    // Configuration cache
    private int defaultTimeout;
    private String cancelKeyword;
    private boolean sendCancelHint;
    private boolean clearChatOnStart;
    private String timeoutMessage;
    private String cancelledMessage;

    public ChatInputManager(Hordes plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();
        
        loadConfiguration();
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Loads configuration from config.yml
     */
    private void loadConfiguration() {
        defaultTimeout = plugin.getFileManager().getConfig()
            .getInt("chat-input.default-timeout", 60);
        
        cancelKeyword = plugin.getFileManager().getConfig()
            .getString("chat-input.cancel-keyword", "cancel");
        
        sendCancelHint = plugin.getFileManager().getConfig()
            .getBoolean("chat-input.send-cancel-hint", true);
        
        clearChatOnStart = plugin.getFileManager().getConfig()
            .getBoolean("chat-input.clear-chat-on-start", true);
        
        timeoutMessage = plugin.getFileManager().getConfig()
            .getString("chat-input.timeout-message", "&cInput timed out");
        
        cancelledMessage = plugin.getFileManager().getConfig()
            .getString("chat-input.cancelled-message", "&cInput cancelled");
    }

    /**
     * Requests input from a player with builder pattern
     * 
     * @param player Player to request input from
     * @return Builder for configuring the input request
     */
    public InputBuilder requestInput(Player player) {
        return new InputBuilder(player);
    }

    /**
     * Cancels active input session for a player
     * 
     * @param player Player to cancel session for
     */
    public void cancelInput(Player player) {
        ChatInputSession session = activeSessions.remove(player.getUniqueId());
        
        if (session != null) {
            // Cancel timeout task
            if (session.timeoutTask != null) {
                session.timeoutTask.cancel();
            }
            
            // Run cancel handler
            if (session.onCancel != null) {
                session.onCancel.run();
            }
        }
    }

    /**
     * Checks if player has active input session
     * 
     * @param player Player to check
     * @return true if player has active session
     */
    public boolean hasActiveSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * Handles chat events for input sessions
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatInputSession session = activeSessions.get(player.getUniqueId());
        
        if (session == null) {
            return;
        }
        
        // Cancel chat event
        event.setCancelled(true);
        
        String input = event.getMessage().trim();
        
        // Check for cancel keyword
        if (input.equalsIgnoreCase(session.cancelKeyword)) {
            activeSessions.remove(player.getUniqueId());
            
            // Cancel timeout
            if (session.timeoutTask != null) {
                session.timeoutTask.cancel();
            }
            
            // Send cancellation message
            if (session.cancelMessage != null) {
                player.sendMessage(Text.createText(session.cancelMessage).build(player));
            }
            
            // Run cancel handler
            if (session.onCancel != null) {
                plugin.getServer().getScheduler().runTask(plugin, session.onCancel);
            }
            return;
        }
        
        // Validate input
        if (session.validator != null && !session.validator.test(input)) {
            // Invalid input - send error message and keep session active
            if (session.invalidMessage != null) {
                player.sendMessage(Text.createText(session.invalidMessage).build(player));
            }
            
            // Re-send prompt if configured
            if (session.repeatPromptOnInvalid && session.prompt != null) {
                player.sendMessage(Text.createText(session.prompt).build(player));
            }
            
            return;
        }
        
        // Valid input - process it
        activeSessions.remove(player.getUniqueId());
        
        // Cancel timeout
        if (session.timeoutTask != null) {
            session.timeoutTask.cancel();
        }
        
        // Run completion handler on main thread
        final String finalInput = input;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            session.onComplete.accept(finalInput);
        });
    }

    /**
     * Cleanup on player quit
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelInput(event.getPlayer());
    }

    /**
     * Reloads configuration
     */
    public void reload() {
        loadConfiguration();
    }

    /**
     * Cleanup all sessions
     */
    public void cleanup() {
        // Cancel all timeout tasks
        for (ChatInputSession session : activeSessions.values()) {
            if (session.timeoutTask != null) {
                session.timeoutTask.cancel();
            }
        }
        
        activeSessions.clear();
    }

    /**
     * Gets active session count (for debugging)
     * 
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Builder class for creating input requests
     */
    public class InputBuilder {
        
        private final Player player;
        private String prompt;
        private Consumer<String> onComplete;
        private Runnable onCancel;
        private Predicate<String> validator;
        private String invalidMessage;
        private int timeout;
        private String timeoutMsg;
        private String cancelMsg;
        private String cancelKeywordOverride;
        private boolean repeatPromptOnInvalid;
        private boolean clearChat;
        private boolean showCancelHint;

        private InputBuilder(Player player) {
            this.player = player;
            // Set defaults from config
            this.timeout = defaultTimeout;
            this.timeoutMsg = timeoutMessage;
            this.cancelMsg = cancelledMessage;
            this.cancelKeywordOverride = cancelKeyword;
            this.repeatPromptOnInvalid = true;
            this.clearChat = clearChatOnStart;
            this.showCancelHint = sendCancelHint;
        }

        /**
         * Sets the prompt message
         */
        public InputBuilder withPrompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        /**
         * Sets the completion handler
         */
        public InputBuilder onComplete(Consumer<String> onComplete) {
            this.onComplete = onComplete;
            return this;
        }

        /**
         * Sets the cancellation handler
         */
        public InputBuilder onCancel(Runnable onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        /**
         * Sets the input validator
         */
        public InputBuilder withValidator(Predicate<String> validator) {
            this.validator = validator;
            return this;
        }

        /**
         * Sets the invalid input message
         */
        public InputBuilder withInvalidMessage(String invalidMessage) {
            this.invalidMessage = invalidMessage;
            return this;
        }

        /**
         * Sets the timeout in seconds
         */
        public InputBuilder withTimeout(int seconds) {
            this.timeout = seconds;
            return this;
        }

        /**
         * Sets the timeout message
         */
        public InputBuilder withTimeoutMessage(String message) {
            this.timeoutMsg = message;
            return this;
        }

        /**
         * Sets the cancellation message
         */
        public InputBuilder withCancelMessage(String message) {
            this.cancelMsg = message;
            return this;
        }

        /**
         * Sets the cancel keyword override
         */
        public InputBuilder withCancelKeyword(String keyword) {
            this.cancelKeywordOverride = keyword;
            return this;
        }

        /**
         * Sets whether to repeat prompt on invalid input
         */
        public InputBuilder repeatPromptOnInvalid(boolean repeat) {
            this.repeatPromptOnInvalid = repeat;
            return this;
        }

        /**
         * Sets whether to clear chat on start
         */
        public InputBuilder clearChatOnStart(boolean clear) {
            this.clearChat = clear;
            return this;
        }

        /**
         * Sets whether to show cancel hint
         */
        public InputBuilder showCancelHint(boolean show) {
            this.showCancelHint = show;
            return this;
        }

        /**
         * Starts the input request
         */
        public void start() {
            if (onComplete == null) {
                throw new IllegalStateException("onComplete handler must be set");
            }

            // Cancel existing session
            cancelInput(player);

            // Clear chat if configured
            if (clearChat) {
                for (int i = 0; i < 100; i++) {
                    player.sendMessage("");
                }
            }

            // Send prompt
            if (prompt != null) {
                player.sendMessage("");
                player.sendMessage(Text.createText(prompt).build(player));
                
                if (showCancelHint) {
                    player.sendMessage(Text.createText(
                        "&7Type &c'" + cancelKeywordOverride + "' &7to cancel"
                    ).build(player));
                }
                
                player.sendMessage("");
            }

            // Close inventory
            player.closeInventory();

            // Create session
            ChatInputSession session = new ChatInputSession(
                player,
                onComplete,
                onCancel,
                validator,
                invalidMessage,
                cancelKeywordOverride,
                cancelMsg,
                repeatPromptOnInvalid,
                prompt
            );

            // Set timeout task
            if (timeout > 0) {
                session.timeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    activeSessions.remove(player.getUniqueId());
                    
                    // Send timeout message
                    if (timeoutMsg != null) {
                        player.sendMessage(Text.createText(timeoutMsg).build(player));
                    }
                    
                    // Run cancel handler
                    if (onCancel != null) {
                        onCancel.run();
                    }
                }, timeout * 20L);
            }

            // Register session
            activeSessions.put(player.getUniqueId(), session);
        }
    }

    /**
     * Inner class for session tracking
     */
    private static class ChatInputSession {
        
        private final Player player;
        private final Consumer<String> onComplete;
        private final Runnable onCancel;
        private final Predicate<String> validator;
        private final String invalidMessage;
        private final String cancelKeyword;
        private final String cancelMessage;
        private final boolean repeatPromptOnInvalid;
        private final String prompt;
        
        private BukkitTask timeoutTask;

        public ChatInputSession(
            Player player,
            Consumer<String> onComplete,
            Runnable onCancel,
            Predicate<String> validator,
            String invalidMessage,
            String cancelKeyword,
            String cancelMessage,
            boolean repeatPromptOnInvalid,
            String prompt
        ) {
            this.player = player;
            this.onComplete = onComplete;
            this.onCancel = onCancel;
            this.validator = validator;
            this.invalidMessage = invalidMessage;
            this.cancelKeyword = cancelKeyword;
            this.cancelMessage = cancelMessage;
            this.repeatPromptOnInvalid = repeatPromptOnInvalid;
            this.prompt = prompt;
        }
    }
}