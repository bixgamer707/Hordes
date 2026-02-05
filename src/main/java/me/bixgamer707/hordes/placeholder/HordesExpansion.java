package me.bixgamer707.hordes.placeholder;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.player.HordePlayer;
import me.bixgamer707.hordes.statistics.PlayerStatistics;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for Hordes
 * Provides comprehensive placeholders for arenas, players, and statistics
 *
 * Usage: %hordes_<placeholder>%
 */
public class HordesExpansion extends PlaceholderExpansion {

    private final Hordes plugin;

    public HordesExpansion(Hordes plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "hordes";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "bixgamer707";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "2.0.0";
    }

    @Override
    public boolean persist() {
        return true; // Keep expansion loaded
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // Player Arena Info
        if (identifier.equals("in_arena")) {
            return plugin.getArenaManager().isInArena(player) ? "Yes" : "No";
        }

        if (identifier.equals("arena")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            return arena != null ? arena.getId() : "None";
        }

        if (identifier.equals("arena_name")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            return arena != null ? arena.getConfig().getDisplayName() : "None";
        }

        if (identifier.equals("arena_state")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            return arena != null ? arena.getState().getDisplayName() : "N/A";
        }

        // Wave Info
        if (identifier.equals("wave")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            return arena != null ? String.valueOf(arena.getCurrentWaveNumber()) : "0";
        }

        if (identifier.equals("total_waves")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            return arena != null ? String.valueOf(arena.getConfig().getTotalWaves()) : "0";
        }

        if (identifier.equals("wave_progress")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            if (arena != null) {
                return arena.getCurrentWaveNumber() + "/" + arena.getConfig().getTotalWaves();
            }
            return "0/0";
        }

        if (identifier.equals("mobs_alive")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            if (arena != null && arena.getCurrentWave() != null) {
                return String.valueOf(arena.getCurrentWave().getMobsAlive());
            }
            return "0";
        }

        if (identifier.equals("mobs_total")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            if (arena != null && arena.getCurrentWave() != null) {
                return String.valueOf(arena.getCurrentWave().getTotalMobs());
            }
            return "0";
        }

        // Player Info
        if (identifier.equals("players")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            return arena != null ? String.valueOf(arena.getPlayerCount()) : "0";
        }

        if (identifier.equals("players_alive")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            return arena != null ? String.valueOf(arena.getAlivePlayerCount()) : "0";
        }

        if (identifier.equals("player_state")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            if (arena != null) {
                HordePlayer hp = arena.getHordePlayer(player.getUniqueId());
                return hp != null ? hp.getState().getDisplayName() : "N/A";
            }
            return "N/A";
        }

        // Session Stats
        if (identifier.equals("session_kills")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            if (arena != null) {
                HordePlayer hp = arena.getHordePlayer(player.getUniqueId());
                return hp != null ? String.valueOf(hp.getKills()) : "0";
            }
            return "0";
        }

        if (identifier.equals("session_deaths")) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            if (arena != null) {
                HordePlayer hp = arena.getHordePlayer(player.getUniqueId());
                return hp != null ? String.valueOf(hp.getDeaths()) : "0";
            }
            return "0";
        }

        // Statistics (if enabled)
        if (plugin.getStatisticsManager() != null && plugin.getStatisticsManager().isEnabled()) {
            PlayerStatistics stats = plugin.getStatisticsManager()
                    .getStatistics(player.getUniqueId(), player.getName());

            if (stats != null) {
                if (identifier.equals("total_kills")) {
                    return String.valueOf(stats.getTotalKills());
                }

                if (identifier.equals("total_deaths")) {
                    return String.valueOf(stats.getTotalDeaths());
                }

                if (identifier.equals("total_completions")) {
                    return String.valueOf(stats.getTotalCompletions());
                }

                if (identifier.equals("total_attempts")) {
                    return String.valueOf(stats.getTotalAttempts());
                }

                if (identifier.equals("win_rate")) {
                    return String.format("%.1f", stats.getWinRate()) + "%";
                }

                if (identifier.equals("kd_ratio")) {
                    return String.format("%.2f", stats.getKDRatio());
                }

                if (identifier.equals("highest_wave")) {
                    return String.valueOf(stats.getHighestWave());
                }

                if (identifier.equals("fastest_time")) {
                    long time = stats.getFastestCompletion();
                    if (time > 0) {
                        return formatTime(time);
                    }
                    return "N/A";
                }

                if (identifier.equals("playtime")) {
                    return formatTime(stats.getTotalPlaytime());
                }
            }
        }

        // Global Info
        if (identifier.equals("total_arenas")) {
            return String.valueOf(plugin.getArenaManager().getArenaCount());
        }

        if (identifier.equals("active_arenas")) {
            return String.valueOf(plugin.getArenaManager().getActiveArenaCount());
        }

        if (identifier.equals("total_players_in_arenas")) {
            return String.valueOf(plugin.getArenaManager().getTotalPlayerCount());
        }

        // Cooldown
        if (identifier.equals("cooldown")) {
            // This would require tracking which arena they last played
            return "N/A";
        }

        // Arena-specific placeholders
        // Format: arena_<id>_<property>
        if (identifier.startsWith("arena_")) {
            String[] parts = identifier.split("_", 3);
            if (parts.length >= 3) {
                String arenaId = parts[1];
                String property = parts[2];
                Arena arena = plugin.getArenaManager().getArena(arenaId);

                if (arena != null) {
                    switch (property) {
                        case "state":
                            return arena.getState().getDisplayName();
                        case "players":
                            return String.valueOf(arena.getPlayerCount());
                        case "maxplayers":
                            return String.valueOf(arena.getConfig().getMaxPlayers());
                        case "wave":
                            return String.valueOf(arena.getCurrentWaveNumber());
                        case "totalwaves":
                            return String.valueOf(arena.getConfig().getTotalWaves());
                    }
                }
            }
        }

        return null; // Placeholder not found
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
}