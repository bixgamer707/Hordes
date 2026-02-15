package me.bixgamer707.hordes.listeners;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.arena.ArenaManager;
import me.bixgamer707.hordes.arena.ArenaState;
import me.bixgamer707.hordes.player.HordePlayer;
import me.bixgamer707.hordes.wave.Wave;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;

import java.util.UUID;

/**
 * Handles all entity-related events
 * - Mob deaths (wave progression)
 * - Entity damage (PvP control)
 * - Entity targeting (arena mob behavior)
 */
public class EntityListener implements Listener {

    private final Hordes plugin;
    private final ArenaManager arenaManager;

    public EntityListener(Hordes plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
    }

    /**
     * Handles entity death events
     * Tracks mob kills for wave progression
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Check if this is an arena mob
        if (!entity.hasMetadata("hordes_arena")) {
            return;
        }

        // Get arena ID
        String arenaId = entity.getMetadata("hordes_arena").get(0).asString();
        Arena arena = arenaManager.getArena(arenaId);

        if (arena == null || arena.getState() != ArenaState.ACTIVE) {
            return;
        }

        // Get killer (if player)
        Player killer = entity.getKiller();

        if (killer != null) {
            HordePlayer hordePlayer = arena.getHordePlayer(killer.getUniqueId());

            if (hordePlayer != null) {
                hordePlayer.addKill();

                // Track statistics
                if (plugin.getStatisticsManager() != null && plugin.getStatisticsManager().isEnabled()) {
                    plugin.getStatisticsManager().getStatistics(killer.getUniqueId(), killer.getName()).addKill(arenaId);
                    plugin.getStatisticsManager().markDirty(killer.getUniqueId());
                }
                /*
                Estas son mis clases para el texto, quiero que todos los mensajes esten en los archivos de mensajes (en_us.yml) no debe haber un solo mensaje por fuera de este archivo asi que actualiza todos los archivos que lo hagan (todos los mensajes que reciba el jugador deben estar para configurar)
                 */
            }
        }

        // Notify wave of mob death
        Wave currentWave = arena.getCurrentWave();

        if (currentWave != null) {
            currentWave.onMobDeath(entity.getUniqueId());
        }

        // Clear drops if configured
        // This prevents arena mobs from dropping items/experience
        // Can be made configurable later
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    /**
     * Handles entity damage by entity
     * Controls PvP and damage mechanics in arenas
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only care about player damage
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Arena victimArena = arenaManager.getPlayerArena(victim);

        if (victimArena == null) {
            return;
        }

        // Check if attacker is a player
        Player attacker = null;

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }

        if (attacker == null) {
            return;
        }

        // Check if attacker is in same arena
        Arena attackerArena = arenaManager.getPlayerArena(attacker);

        if (attackerArena != victimArena) {
            // Different arenas or attacker not in arena - cancel
            event.setCancelled(true);
            return;
        }

        // Check PvP setting
        if (!victimArena.getConfig().getSurvivalMode().isPvPAllowed()) {
            event.setCancelled(true);
        }
    }

    /**
     * Handles entity target events
     * Ensures arena mobs only target arena players
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();

        // Check if this is an arena mob
        if (!entity.hasMetadata("hordes_arena")) {
            return;
        }

        // If target is not a player, allow
        if (!(event.getTarget() instanceof Player)) {
            return;
        }

        Player targetPlayer = (Player) event.getTarget();

        // Get arena
        String arenaId = entity.getMetadata("hordes_arena").get(0).asString();
        Arena arena = arenaManager.getArena(arenaId);

        if (arena == null) {
            event.setCancelled(true);
            return;
        }

        // Check if target is in this arena
        if (!arena.hasPlayer(targetPlayer.getUniqueId())) {
            // Target not in arena - cancel
            event.setCancelled(true);
        }
    }

    /**
     * Handles entity spawn events
     * Prevents natural spawns in arena areas (optional)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        // This is optional - could prevent natural spawns in arena regions
        // For now, we'll skip this to allow natural spawns
        // Can be implemented later with WorldGuard region checks
    }

    /**
     * Handles entity explosion events
     * Controls creeper/TNT explosions in arenas
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();

        // Check if this is an arena mob
        if (!entity.hasMetadata("hordes_arena")) {
            return;
        }

        // Prevent block damage from arena mobs
        // This prevents creepers from destroying the arena
        event.blockList().clear();
    }

    /**
     * Handles entity damage events
     * Prevents arena mobs from taking damage outside arena
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        // Check if this is an arena mob
        if (!entity.hasMetadata("hordes_arena")) {
            return;
        }

        // Get arena
        String arenaId = entity.getMetadata("hordes_arena").get(0).asString();
        Arena arena = arenaManager.getArena(arenaId);

        if (arena == null || arena.getState() != ArenaState.ACTIVE) {
            // Arena not active - remove mob
            entity.remove();
            event.setCancelled(true);
        }
    }

    /**
     * Handles entity combustion (fire damage)
     * Prevents arena mobs from burning in daylight
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent event) {
        Entity entity = event.getEntity();

        // Check if this is an arena mob
        if (!entity.hasMetadata("hordes_arena")) {
            return;
        }

        // Prevent arena mobs from burning
        event.setCancelled(true);
    }
}