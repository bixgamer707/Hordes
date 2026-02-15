package me.bixgamer707.hordes.arena;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.config.*;
import me.bixgamer707.hordes.player.HordePlayer;
import me.bixgamer707.hordes.player.PlayerState;
import me.bixgamer707.hordes.statistics.PlayerStatistics;
import me.bixgamer707.hordes.wave.Wave;
import me.bixgamer707.hordes.wave.WaveManager;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a complete arena/dungeon instance
 * Handles the full lifecycle: lobby → countdown → active → completion

 * Thread-safe implementation with concurrent collections
 * Optimized for performance with minimal object creation
 */
public class Arena {

    private final String id;
    private final ArenaConfig config;
    private final Hordes plugin;
    
    // State management
    private volatile ArenaState state;
    
    // Player management (thread-safe)
    private final Map<UUID, HordePlayer> players;
    private final Set<UUID> alivePlayers;
    private final Set<UUID> deadPlayers;
    
    // Wave management
    private final WaveManager waveManager;
    private Wave currentWave;
    private int currentWaveNumber;
    private boolean waitingForManualProgression;
    
    // Scheduled tasks
    private BukkitTask countdownTask;
    private BukkitTask waveDelayTask;
    
    // Performance tracking
    private long startTime;
    private long lastWaveStartTime;

    public Arena(String id, ArenaConfig config, Hordes plugin) {
        this.id = id;
        this.config = config;
        this.plugin = plugin;
        this.state = ArenaState.WAITING;
        
        // Use concurrent collections for thread-safety
        this.players = new ConcurrentHashMap<>();
        this.alivePlayers = ConcurrentHashMap.newKeySet();
        this.deadPlayers = ConcurrentHashMap.newKeySet();
        
        this.waveManager = new WaveManager(this, config);
        this.currentWaveNumber = 0;
        this.waitingForManualProgression = false;
    }

    /**
     * Attempts to add a player to the arena
     * 
     * @param player Player to add
     * @return true if successfully joined
     */
    public boolean joinPlayer(Player player) {
        // Pre-validation checks
        if (!canJoin(player)) {
            return false;
        }
        
        // Create player wrapper
        HordePlayer hordePlayer = new HordePlayer(player, this);
        
        // Start statistics session
        if (plugin.getStatisticsManager() != null && plugin.getStatisticsManager().isEnabled()) {
            PlayerStatistics stats = plugin.getStatisticsManager()
                .getStatistics(player.getUniqueId(), player.getName());
            if (stats != null) {
                stats.startSession();
                stats.addAttempt(id);
                plugin.getStatisticsManager().markDirty(player.getUniqueId());
            }
        }
        
        // Handle inventory based on survival mode
        handlePlayerJoin(player, hordePlayer);
        
        // Add to tracking collections
        players.put(player.getUniqueId(), hordePlayer);
        alivePlayers.add(player.getUniqueId());
        hordePlayer.setState(PlayerState.LOBBY);
        
        // Start session tracking
        if (plugin.getStatisticsManager() != null && plugin.getStatisticsManager().isEnabled()) {
            plugin.getStatisticsManager().getStatistics(player.getUniqueId(), player.getName()).startSession();
        }
        
        // Teleport to lobby
        player.teleport(config.getLobbySpawn());
        
        // Play join sound
        plugin.getSoundManager().playJoin(player);
        
        // Apply lobby state
        applyLobbyState(player);
        
        // Show boss bar (will be updated when arena starts)
        plugin.getBossBarManager().showBossBar(player, id);
        
        // Broadcast join message
        broadcastMessage("arena.player-joined", 
            player.getName(), 
            players.size(), 
            config.getMaxPlayers()
        );
        
        // Check for auto-start
        checkAutoStart();
        
        return true;
    }

    /**
     * Handles player join based on survival mode configuration
     */
    private void handlePlayerJoin(Player player, HordePlayer hordePlayer) {
        ArenaConfig.SurvivalModeConfig survivalMode = config.getSurvivalMode();
        
        if (survivalMode.shouldSaveInventory()) {
            // Save original state for restoration
            hordePlayer.saveState();
        }
        
        if (survivalMode.shouldClearInventory()) {
            // Clear inventory for arena mode
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }
        
        // Apply gamemode if configured
        if (survivalMode.shouldForceGameMode()) {
            player.setGameMode(survivalMode.getGameMode());
        }
        
        // Reset health and hunger
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        
        // Clear potion effects if not survival mode
        if (!survivalMode.isEnabled()) {
            player.getActivePotionEffects().forEach(effect -> 
                player.removePotionEffect(effect.getType())
            );
        }
    }

    /**
     * Applies lobby state to player (adventure mode, no flight, etc.)
     */
    private void applyLobbyState(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    /**
     * Validates if a player can join
     */
    public boolean canJoin(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Already in arena
        if (players.containsKey(uuid)) {
            sendMessage(player, "arena.already-joined");
            return false;
        }
        
        // Arena not joinable
        if (!state.isJoinable()) {
            sendMessage(player, "arena.not-joinable");
            return false;
        }
        
        // Arena disabled
        if (!config.isEnabled()) {
            sendMessage(player, "arena.disabled");
            return false;
        }
        
        // Arena full
        if (players.size() >= config.getMaxPlayers()) {
            sendMessage(player, "arena.full");
            return false;
        }
        
        // Cooldown check
        if (plugin.getCooldownManager().hasCooldown(uuid, id)) {
            String remaining = plugin.getCooldownManager().getRemainingTime(uuid, id);
            sendMessage(player, "arena.cooldown", remaining);
            return false;
        }
        
        // Permission check
        if (!player.hasPermission("hordes.join." + id)) {
            sendMessage(player, "arena.no-permission");
            return false;
        }
        
        return true;
    }

    /**
     * Removes a player from the arena
     * 
     * @param player Player to remove
     * @param restore Whether to restore original state
     */
    public void removePlayer(Player player, boolean restore) {
        UUID uuid = player.getUniqueId();
        HordePlayer hordePlayer = players.remove(uuid);
        
        if (hordePlayer == null) {
            return;
        }
        
        // Hide boss bar
        plugin.getBossBarManager().hideBossBar(player);
        
        // Remove from tracking sets
        alivePlayers.remove(uuid);
        deadPlayers.remove(uuid);
        
        // Restore player state if needed
        if (restore && config.getSurvivalMode().shouldSaveInventory()) {
            hordePlayer.restoreState();
        }
        
        // Teleport to exit
        player.teleport(config.getExitLocation());
        
        // Play leave sound
        plugin.getSoundManager().playLeave(player);
        
        // Reset to normal state
        if (config.getSurvivalMode().shouldForceGameMode()) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        
        // Broadcast leave message
        broadcastMessage("arena.player-left", 
            player.getName(), 
            players.size(), 
            config.getMaxPlayers()
        );
        
        // Check if arena should end
        checkArenaEnd();
    }

    /**
     * Handles player death in the arena
     */
    public void onPlayerDeath(HordePlayer hordePlayer) {
        UUID uuid = hordePlayer.getUuid();
        Player player = hordePlayer.getPlayer();
        
        if (player == null) {
            return;
        }
        
        // Move from alive to dead
        alivePlayers.remove(uuid);
        deadPlayers.add(uuid);
        
        // Update state
        hordePlayer.setState(PlayerState.DEAD);
        hordePlayer.addDeath();
        
        // Track statistics
        if (plugin.getStatisticsManager() != null && plugin.getStatisticsManager().isEnabled()) {
            plugin.getStatisticsManager().getStatistics(uuid, player.getName()).addDeath(id);
            plugin.getStatisticsManager().markDirty(uuid);
        }
        
        // Play death sound
        plugin.getSoundManager().playPlayerDeath(player);
        
        // Get death handling config
        ArenaConfig.DeathHandlingConfig deathConfig = config.getDeathHandling();
        
        // Broadcast death
        broadcastMessage("arena.player-died", 
            player.getName(), 
            alivePlayers.size()
        );
        
        // Handle based on configuration
        switch (deathConfig.getAction()) {
            case KICK:
                handleDeathKick(player, hordePlayer);
                break;
                
            case SPECTATE:
                handleDeathSpectate(player, hordePlayer);
                break;
                
            case REJOIN:
                handleDeathRejoin(player, hordePlayer);
                break;
                
            case RESPAWN:
                handleDeathRespawn(player, hordePlayer);
                break;
        }
        
        // Check if all players are dead
        if (alivePlayers.isEmpty()) {
            endArena(false); // Defeat
        }
    }

    /**
     * Handles KICK death action
     */
    private void handleDeathKick(Player player, HordePlayer hordePlayer) {
        // Schedule removal after a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                removePlayer(player, true);
                sendMessage(player, "arena.death-kicked");
            }
        }.runTaskLater(plugin, 20L); // 1 second delay
    }

    /**
     * Handles SPECTATE death action
     */
    private void handleDeathSpectate(Player player, HordePlayer hordePlayer) {
        hordePlayer.setState(PlayerState.SPECTATING);
        
        // Teleport back to arena (after respawn)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR);
                    
                    if (config.getDeathHandling().shouldTeleport()) {
                        player.teleport(config.getExitLocation());
                    } else {
                        player.teleport(config.getArenaSpawn());
                    }
                    
                    sendMessage(player, "arena.death-spectate");
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    /**
     * Handles REJOIN death action
     */
    private void handleDeathRejoin(Player player, HordePlayer hordePlayer) {
        int rejoinCooldown = config.getDeathHandling().getRejoinCooldown();
        
        // Temporarily remove from arena
        if (config.getDeathHandling().shouldTeleport()) {
            player.teleport(config.getExitLocation());
        }
        
        // Set temporary cooldown for rejoin
        if (rejoinCooldown > 0) {
            plugin.getCooldownManager().setTempCooldown(
                player.getUniqueId(), 
                id, 
                rejoinCooldown
            );
        }
        
        sendMessage(player, "arena.death-rejoin", String.valueOf(rejoinCooldown));
    }

    /**
     * Handles RESPAWN death action
     */
    private void handleDeathRespawn(Player player, HordePlayer hordePlayer) {
        // Respawn at arena spawn after delay
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && state == ArenaState.ACTIVE) {
                    player.teleport(config.getArenaSpawn());
                    player.setHealth(player.getMaxHealth());
                    player.setFoodLevel(20);
                    
                    // Move back to alive
                    deadPlayers.remove(player.getUniqueId());
                    alivePlayers.add(player.getUniqueId());
                    hordePlayer.setState(PlayerState.PLAYING);
                    
                    sendMessage(player, "arena.death-respawned");
                }
            }
        }.runTaskLater(plugin, 60L); // 3 seconds
    }

    /**
     * Checks if arena should auto-start
     */
    private void checkAutoStart() {
        if (!config.isAutoStart()) {
            return;
        }
        
        if (state == ArenaState.WAITING && players.size() >= config.getMinPlayers()) {
            startCountdown();
        } else if (state == ArenaState.STARTING && players.size() < config.getMinPlayers()) {
            cancelCountdown();
        }
    }

    /**
     * Starts the countdown before arena begins
     */
    private void startCountdown() {
        state = ArenaState.STARTING;
        
        final int countdownTime = config.getCountdownTime();
        
        countdownTask = new BukkitRunnable() {
            int timeLeft = countdownTime;
            
            @Override
            public void run() {
                // Recheck minimum players
                if (players.size() < config.getMinPlayers()) {
                    cancelCountdown();
                    return;
                }
                
                if (timeLeft <= 0) {
                    cancel();
                    startArena();
                    return;
                }
                
                // Broadcast at specific intervals
                if (timeLeft <= 5 || timeLeft == 10 || timeLeft == 15 || timeLeft == 30) {
                    broadcastMessage("arena.countdown", timeLeft);
                    
                    // Play countdown sound
                    plugin.getSoundManager().playCountdownId(alivePlayers, timeLeft);
                }
                
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Cancels the countdown
     */
    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        
        state = ArenaState.WAITING;
        broadcastMessage("arena.countdown-cancelled");
    }

    /**
     * Starts the arena (begins wave 1)
     */
    private void startArena() {
        state = ArenaState.ACTIVE;
        startTime = System.currentTimeMillis();
        
        // Teleport all players to arena
        for (HordePlayer hp : players.values()) {
            Player p = hp.getPlayer();
            if (p != null) {
                p.teleport(config.getArenaSpawn());
                
                // Apply game mode
                if (config.getSurvivalMode().shouldForceGameMode()) {
                    p.setGameMode(config.getSurvivalMode().getGameMode());
                }
                
                hp.setState(PlayerState.PLAYING);
            }
        }
        
        // Broadcast start
        broadcastMessage("arena.started");
        
        // Start first wave
        startWave(1);
    }

    /**
     * Starts a specific wave
     */
    public void startWave(int waveNumber) {
        currentWaveNumber = waveNumber;
        lastWaveStartTime = System.currentTimeMillis();
        waitingForManualProgression = false;
        
        // Create wave
        currentWave = waveManager.createWave(waveNumber);
        
        if (currentWave == null) {
            // No more waves - victory!
            endArena(true);
            return;
        }
        
        // Broadcast wave start
        broadcastMessage("arena.wave-starting", 
            waveNumber, 
            config.getTotalWaves()
        );
        
        // Play sound
        plugin.getSoundManager().playWaveStartId(alivePlayers);
        
        // Update boss bar
        plugin.getBossBarManager().updateBossBar(this);
        
        // Start wave
        currentWave.start();
    }

    /**
     * Called when current wave is completed
     */
    public void onWaveComplete() {
        long waveDuration = (System.currentTimeMillis() - lastWaveStartTime) / 1000;
        
        broadcastMessage("arena.wave-completed", 
            currentWaveNumber, 
            waveDuration
        );
        
        // Play sound
        plugin.getSoundManager().playWaveCompleteId(alivePlayers);
        
        // Update boss bar
        plugin.getBossBarManager().updateBossBar(this);
        
        // Give progressive rewards if configured
        if (config.getRewardConfig().getType() != RewardType.COMPLETION_ONLY) {
            giveProgressiveRewards();
        }
        
        // Check progression type
        switch (config.getProgressionType()) {
            case AUTOMATIC:
                scheduleNextWave();
                break;
                
            case MANUAL:
                waitForManualProgression();
                break;
                
            case MIXED:
                // Check wave-specific config (future implementation)
                scheduleNextWave();
                break;
        }
    }

    /**
     * Schedules the next wave after delay
     */
    private void scheduleNextWave() {
        int delay = config.getWaveDelay();
        
        if (delay > 0) {
            broadcastMessage("arena.next-wave-in", delay);
            
            waveDelayTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (state == ArenaState.ACTIVE) {
                        startWave(currentWaveNumber + 1);
                    }
                }
            }.runTaskLater(plugin, delay * 20L);
        } else {
            startWave(currentWaveNumber + 1);
        }
    }

    /**
     * Waits for manual progression (button/sign interaction)
     */
    private void waitForManualProgression() {
        waitingForManualProgression = true;
        broadcastMessage("arena.waiting-for-progression");
    }

    /**
     * Manually triggers next wave (called from listener)
     */
    public void triggerNextWave() {
        if (waitingForManualProgression && state == ArenaState.ACTIVE) {
            startWave(currentWaveNumber + 1);
        }
    }

    /**
     * Ends the arena
     * 
     * @param victory true if players won
     */
    public void endArena(boolean victory) {
        state = ArenaState.ENDING;
        
        long totalDuration = (System.currentTimeMillis() - startTime) / 1000;
        
        // Cancel any running tasks
        cleanup();
        
        if (victory) {
            broadcastMessage("arena.victory", totalDuration);
            
            // Play victory sound
            plugin.getSoundManager().playVictoryId(alivePlayers);
            
            giveCompletionRewards();
            
            // Track statistics for winners
            if (plugin.getStatisticsManager() != null && plugin.getStatisticsManager().isEnabled()) {
                for (UUID uuid : alivePlayers) {
                    HordePlayer hp = players.get(uuid);
                    if (hp != null) {
                        Player p = hp.getPlayer();
                        if (p != null) {
                            plugin.getStatisticsManager().getStatistics(uuid, p.getName())
                                .addCompletion(id, currentWaveNumber, totalDuration);
                            plugin.getStatisticsManager().markDirty(uuid);
                        }
                    }
                }
            }
        } else {
            broadcastMessage("arena.defeat", totalDuration);
            
            // Play defeat sound
            plugin.getSoundManager().playDefeatId(alivePlayers);
        }
        
        // End session and track playtime for all players
        if (plugin.getStatisticsManager() != null && plugin.getStatisticsManager().isEnabled()) {
            for (UUID uuid : players.keySet()) {
                HordePlayer hp = players.get(uuid);
                if (hp != null) {
                    Player p = hp.getPlayer();
                    if (p != null) {
                        plugin.getStatisticsManager().getStatistics(uuid, p.getName()).endSession(id);
                        plugin.getStatisticsManager().getStatistics(uuid, p.getName()).addAttempt(id);
                        plugin.getStatisticsManager().markDirty(uuid);
                    }
                }
            }
        }
        
        // Apply cooldowns
        applyCooldowns();
        
        // Schedule player removal and reset
        new BukkitRunnable() {
            @Override
            public void run() {
                // Remove all players
                for (HordePlayer hp : new ArrayList<>(players.values())) {
                    Player p = hp.getPlayer();
                    if (p != null) {
                        removePlayer(p, true);
                    }
                }
                
                // Remove boss bar
                plugin.getBossBarManager().removeBossBar(id);
                
                // Reset arena
                reset();
            }
        }.runTaskLater(plugin, 100L); // 5 seconds
    }

    /**
     * Gives progressive rewards (per wave)
     */
    private void giveProgressiveRewards() {
        ArenaConfig.RewardConfig rewardConfig = config.getRewardConfig();
        double multiplier = rewardConfig.getProgressiveMultiplier();
        
        for (UUID uuid : alivePlayers) {
            HordePlayer hp = players.get(uuid);
            if (hp != null && hp.getPlayer() != null) {
                plugin.getRewardManager().giveProgressiveReward(
                    hp.getPlayer(), 
                    rewardConfig, 
                    multiplier
                );
            }
        }
    }

    /**
     * Gives completion rewards (full arena completion)
     */
    private void giveCompletionRewards() {
        ArenaConfig.RewardConfig rewardConfig = config.getRewardConfig();
        
        if (!rewardConfig.isEnabled()) {
            return;
        }
        
        for (UUID uuid : alivePlayers) {
            HordePlayer hp = players.get(uuid);
            if (hp != null && hp.getPlayer() != null) {
                plugin.getRewardManager().giveCompletionReward(
                    hp.getPlayer(), 
                    rewardConfig
                );
            }
        }
    }

    /**
     * Applies cooldowns to all players
     */
    private void applyCooldowns() {
        long cooldownDuration = config.getCooldownDuration();
        
        if (cooldownDuration <= 0) {
            return;
        }
        
        for (UUID uuid : players.keySet()) {
            if (config.isGlobalCooldown()) {
                plugin.getCooldownManager().setGlobalCooldown(uuid, cooldownDuration);
            } else {
                plugin.getCooldownManager().setCooldown(uuid, id, cooldownDuration);
            }
        }
    }

    /**
     * Cleans up all arena resources
     */
    private void cleanup() {
        // Cancel tasks
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        
        if (waveDelayTask != null) {
            waveDelayTask.cancel();
            waveDelayTask = null;
        }
        
        // Cleanup current wave
        if (currentWave != null) {
            currentWave.cleanup();
            currentWave = null;
        }
    }

    /**
     * Resets arena to initial state
     */
    private void reset() {
        state = ArenaState.WAITING;
        currentWaveNumber = 0;
        currentWave = null;
        waitingForManualProgression = false;
        
        players.clear();
        alivePlayers.clear();
        deadPlayers.clear();
    }

    /**
     * Checks if arena should end (no alive players)
     */
    private void checkArenaEnd() {
        if (state == ArenaState.ACTIVE && alivePlayers.isEmpty()) {
            endArena(false);
        } else if ((state == ArenaState.WAITING || state == ArenaState.STARTING) 
                   && players.isEmpty()) {
            cancelCountdown();
        }
    }

    /**
     * Broadcasts a message to all arena players
     */
    public void broadcastMessage(String path, Object... replacements) {
        // Get message and replace placeholders
        String message = Text.getMessages().getString("Messages." + path, path);
        
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
        }
        
        // Broadcast to all arena players with PlaceholderAPI per player
        for (UUID uuid : alivePlayers) {

            Player player = Bukkit.getPlayer(uuid);

            if(player == null) continue;

            player.sendMessage(Text.createText(message).build(player));
        }
    }

    /**
     * Sends a message to a player
     */
    private void sendMessage(Player player, String path, Object... replacements) {
        // Get message and replace placeholders
        String message = Text.getMessages().getString("Messages." + path, path);
        
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
        }
        
        // Use Text to colorize and apply PlaceholderAPI
        player.sendMessage(Text.createText(message).build(player));
    }


    // Getters
    public String getId() { return id; }
    public ArenaConfig getConfig() { return config; }
    public ArenaState getState() { return state; }
    public Map<UUID, HordePlayer> getPlayers() { return new HashMap<>(players); }
    public Set<UUID> getAlivePlayers() { return new HashSet<>(alivePlayers); }
    public Set<UUID> getDeadPlayers() { return new HashSet<>(deadPlayers); }
    public int getCurrentWaveNumber() { return currentWaveNumber; }
    public Wave getCurrentWave() { return currentWave; }
    public Hordes getPlugin() { return plugin; }
    public boolean isWaitingForManualProgression() { return waitingForManualProgression; }
    
    /**
     * Gets player count (thread-safe)
     */
    public int getPlayerCount() {
        return players.size();
    }
    
    /**
     * Gets alive player count (thread-safe)
     */
    public int getAlivePlayerCount() {
        return alivePlayers.size();
    }
    
    /**
     * Checks if player is in arena
     */
    public boolean hasPlayer(UUID uuid) {
        return players.containsKey(uuid);
    }
    
    /**
     * Gets HordePlayer wrapper
     */
    public HordePlayer getHordePlayer(UUID uuid) {
        return players.get(uuid);
    }
}
