package me.bixgamer707.hordes.sound;

import me.bixgamer707.hordes.Hordes;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages sound effects for arena events
 * Plays sounds based on configuration
 */
public class SoundManager {

    private final Hordes plugin;
    private boolean enabled;

    public SoundManager(Hordes plugin) {
        this.plugin = plugin;
        checkEnabled();
    }

    /**
     * Checks if sounds are enabled
     */
    private void checkEnabled() {
        enabled = plugin.getFileManager().getConfig()
            .getBoolean("sounds.enabled", true);
    }

    /**
     * Plays wave start sound
     */
    public void playWaveStart(Collection<Player> players) {
        if (!enabled) return;

        String soundName = plugin.getFileManager().getConfig()
                .getString("sounds.wave-start", "ENTITY_ENDER_DRAGON_GROWL");

        playSound(players, soundName, 1.0f, 1.0f);
    }

    public void playWaveStartId(Collection<UUID> playersId) {
        if (!enabled) return;

        String soundName = plugin.getFileManager().getConfig()
                .getString("sounds.wave-start", "ENTITY_ENDER_DRAGON_GROWL");

        Set<Player> players = new HashSet<>();

        playersId.forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);

            if(player!=null) players.add(player);
        });

        playSound(players, soundName, 1.0f, 1.0f);
    }



    /**
     * Plays wave complete sound
     */
    public void playWaveComplete(Collection<Player> players) {
        if (!enabled) return;

        String soundName = plugin.getFileManager().getConfig()
                .getString("sounds.wave-complete", "ENTITY_PLAYER_LEVELUP");

        playSound(players, soundName, 1.0f, 1.0f);
    }

    public void playWaveCompleteId(Collection<UUID> playersId) {
        if (!enabled) return;

        String soundName = plugin.getFileManager().getConfig()
                .getString("sounds.wave-complete", "ENTITY_PLAYER_LEVELUP");

        Set<Player> players = new HashSet<>();

        playersId.forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);

            if(player!=null) players.add(player);
        });

        playSound(players, soundName, 1.0f, 1.0f);
    }

    /**
     * Plays arena victory sound
     */
    public void playVictory(Collection<Player> players) {
        if (!enabled) return;

        String soundName = plugin.getFileManager().getConfig()
                .getString("sounds.arena-victory", "UI_TOAST_CHALLENGE_COMPLETE");

        playSound(players, soundName, 1.0f, 1.0f);
    }

    public void playVictoryId(Collection<UUID> playersId) {
        if (!enabled) return;

        String soundName = plugin.getFileManager().getConfig()
                .getString("sounds.arena-victory", "UI_TOAST_CHALLENGE_COMPLETE");

        Set<Player> players = new HashSet<>();

        playersId.forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);

            if(player!=null) players.add(player);
        });

        playSound(players, soundName, 1.0f, 1.0f);
    }

    /**
     * Plays arena defeat sound
     */
    public void playDefeat(Collection<Player> players) {
        if (!enabled) return;

        String soundName = plugin.getFileManager().getConfig()
                .getString("sounds.arena-defeat", "ENTITY_WITHER_DEATH");

        playSound(players, soundName, 0.8f, 0.8f);
    }

    public void playDefeatId(Collection<UUID> playersId) {
        if (!enabled) return;

        String soundName = plugin.getFileManager().getConfig()
                .getString("sounds.arena-defeat", "ENTITY_WITHER_DEATH");

        Set<Player> players = new HashSet<>();

        playersId.forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);

            if(player!=null) players.add(player);
        });

        playSound(players, soundName, 1.0f, 1.0f);
    }



    /**
     * Plays player death sound
     */
    public void playPlayerDeath(Player player) {
        if (!enabled) return;

        String soundName = plugin.getFileManager().getConfig()
                .getString("sounds.player-death", "ENTITY_PLAYER_DEATH");

        playSound(player, soundName, 1.0f, 1.0f);
    }


    /**
     * Plays countdown sound
     */
    public void playCountdown(Collection<Player> players, int seconds) {
        if (!enabled) return;

        Sound sound;
        float pitch;

        if (seconds <= 3) {
            sound = Sound.BLOCK_NOTE_BLOCK_PLING;
            pitch = seconds == 1 ? 2.0f : 1.5f;
        } else {
            sound = Sound.BLOCK_NOTE_BLOCK_BASS;
            pitch = 1.0f;
        }

        for (Player player : players) {
            player.playSound(player.getLocation(), sound, 1.0f, pitch);
        }
    }

    public void playCountdownId(Collection<UUID> playersId, int seconds) {
        if (!enabled) return;

        Sound sound;
        float pitch;

        if (seconds <= 3) {
            sound = Sound.BLOCK_NOTE_BLOCK_PLING;
            pitch = seconds == 1 ? 2.0f : 1.5f;
        } else {
            sound = Sound.BLOCK_NOTE_BLOCK_BASS;
            pitch = 1.0f;
        }

        for (UUID player : playersId) {

            Player p =  Bukkit.getPlayer(player);

            if (p == null) continue;

            p.playSound(p.getLocation(), sound, 1.0f, pitch);
        }
    }

    /**
     * Plays join sound
     */
    public void playJoin(Player player) {
        if (!enabled) return;
        
        playSound(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f);
    }

    /**
     * Plays leave sound
     */
    public void playLeave(Player player) {
        if (!enabled) return;
        
        playSound(player, "BLOCK_PORTAL_TRAVEL", 0.5f, 1.5f);
    }

    /**
     * Plays sound to multiple players
     */
    private void playSound(Collection<Player> players, String soundName, float volume, float pitch) {
        Sound sound = parseSound(soundName);
        
        if (sound != null) {
            for (Player player : players) {
                try {
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } catch (Exception e) {
                    // Ignore sound errors
                }
            }
        }
    }

    /**
     * Plays sound to single player
     */
    private void playSound(Player player, String soundName, float volume, float pitch) {
        Sound sound = parseSound(soundName);
        
        if (sound != null) {
            try {
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception e) {
                // Ignore sound errors
            }
        }
    }

    /**
     * Parses sound from string
     */
    private Sound parseSound(String soundName) {
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.logWarning("Invalid sound: " + soundName);
            return null;
        }
    }

    /**
     * Reloads sound configuration
     */
    public void reload() {
        checkEnabled();
    }

    /**
     * Checks if sounds are enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}