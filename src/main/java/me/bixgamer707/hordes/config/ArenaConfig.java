package me.bixgamer707.hordes.config;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración completa de una arena/dungeon
 * Maneja todos los aspectos configurables del sistema
 */
public class ArenaConfig {

    private final String id;
    
    // Información básica
    private String displayName;
    private boolean enabled;
    
    // Límites de jugadores
    private int minPlayers;
    private int maxPlayers;
    
    // Locations
    private Location lobbySpawn;
    private Location arenaSpawn;
    private Location exitLocation;
    
    // Waves
    private int totalWaves;
    private int waveDelay; // segundos entre waves
    private WaveProgressionType progressionType;
    
    // Countdown
    private int countdownTime;
    private boolean autoStart;
    
    // Cooldown
    private long cooldownDuration; // segundos
    private boolean globalCooldown; // aplica a todas las arenas
    
    // Modo Survival (CLAVE PARA TU SISTEMA)
    private SurvivalModeConfig survivalMode;
    
    // Death handling (configurable)
    private DeathHandlingConfig deathHandling;
    
    // Item handling (configurable)
    private ItemHandlingConfig itemHandling;
    
    // Recompensas
    private RewardConfig rewardConfig;
    
    // WorldGuard (opcional)
    private String worldGuardRegion;
    
    public ArenaConfig(String id) {
        this.id = id;
        this.enabled = true;
        this.minPlayers = 1;
        this.maxPlayers = 4;
        this.totalWaves = 5;
        this.waveDelay = 10;
        this.countdownTime = 10;
        this.autoStart = true;
        this.cooldownDuration = 3600;
        this.globalCooldown = false;
        this.progressionType = WaveProgressionType.AUTOMATIC;
        
        // Configs por defecto
        this.survivalMode = new SurvivalModeConfig();
        this.deathHandling = new DeathHandlingConfig();
        this.itemHandling = new ItemHandlingConfig();
        this.rewardConfig = new RewardConfig();
    }

    /**
     * Carga la configuración desde un ConfigurationSection
     */
    public static ArenaConfig load(String id, ConfigurationSection section) {
        ArenaConfig config = new ArenaConfig(id);
        
        if (section == null) {
            Bukkit.getLogger().warning("[Hordes] No se encontró configuración para arena: " + id);
            return config;
        }
        
        // Información básica
        config.displayName = section.getString("display-name", id);
        config.enabled = section.getBoolean("enabled", true);
        
        // Límites
        config.minPlayers = section.getInt("min-players", 1);
        config.maxPlayers = section.getInt("max-players", 4);
        
        // Locations
        config.lobbySpawn = loadLocation(section.getConfigurationSection("lobby-spawn"));
        config.arenaSpawn = loadLocation(section.getConfigurationSection("arena-spawn"));
        config.exitLocation = loadLocation(section.getConfigurationSection("exit-location"));
        
        // Waves
        config.totalWaves = section.getInt("waves", 5);
        config.waveDelay = section.getInt("wave-delay", 10);
        
        // Progresión
        String progressionStr = section.getString("wave-progression", "AUTOMATIC");
        try {
            config.progressionType = WaveProgressionType.valueOf(progressionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            config.progressionType = WaveProgressionType.AUTOMATIC;
        }
        
        // Countdown
        config.countdownTime = section.getInt("countdown-time", 10);
        config.autoStart = section.getBoolean("auto-start", true);
        
        // Cooldown
        config.cooldownDuration = section.getLong("cooldown", 3600);
        config.globalCooldown = section.getBoolean("global-cooldown", false);
        
        // WorldGuard
        config.worldGuardRegion = section.getString("worldguard-region");
        
        // Survival Mode
        config.survivalMode = SurvivalModeConfig.load(
            section.getConfigurationSection("survival-mode")
        );
        
        // Death Handling
        config.deathHandling = DeathHandlingConfig.load(
            section.getConfigurationSection("death-handling")
        );
        
        // Item Handling
        config.itemHandling = ItemHandlingConfig.load(
            section.getConfigurationSection("item-handling")
        );
        
        // Rewards
        config.rewardConfig = RewardConfig.load(
            section.getConfigurationSection("rewards")
        );
        
        return config;
    }

    /**
     * Carga una Location desde configuración
     */
    private static Location loadLocation(ConfigurationSection section) {
        if (section == null) return null;
        
        try {
            String worldName = section.getString("world");
            World world = Bukkit.getWorld(worldName);
            
            if (world == null) {
                Bukkit.getLogger().warning("[Hordes] Mundo no encontrado: " + worldName);
                return null;
            }
            
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            float yaw = (float) section.getDouble("yaw", 0);
            float pitch = (float) section.getDouble("pitch", 0);
            
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            Bukkit.getLogger().severe("[Hordes] Error cargando location: " + e.getMessage());
            return null;
        }
    }

    /**
     * Valida que la configuración sea válida
     */
    public boolean isValid() {
        if (lobbySpawn == null) {
            Bukkit.getLogger().warning("[Hordes] Arena " + id + " no tiene lobby-spawn configurado");
            return false;
        }
        
        if (arenaSpawn == null) {
            Bukkit.getLogger().warning("[Hordes] Arena " + id + " no tiene arena-spawn configurado");
            return false;
        }
        
        if (exitLocation == null) {
            Bukkit.getLogger().warning("[Hordes] Arena " + id + " no tiene exit-location configurado");
            return false;
        }
        
        if (minPlayers < 1) {
            Bukkit.getLogger().warning("[Hordes] Arena " + id + " tiene min-players inválido");
            return false;
        }
        
        if (maxPlayers < minPlayers) {
            Bukkit.getLogger().warning("[Hordes] Arena " + id + " tiene max-players < min-players");
            return false;
        }
        
        if (totalWaves < 1) {
            Bukkit.getLogger().warning("[Hordes] Arena " + id + " no tiene waves configuradas");
            return false;
        }
        
        return true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public boolean isEnabled() { return enabled; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public Location getLobbySpawn() { return lobbySpawn; }
    public Location getArenaSpawn() { return arenaSpawn; }
    public Location getExitLocation() { return exitLocation; }
    public int getTotalWaves() { return totalWaves; }
    public int getWaveDelay() { return waveDelay; }
    public WaveProgressionType getProgressionType() { return progressionType; }
    public int getCountdownTime() { return countdownTime; }
    public boolean isAutoStart() { return autoStart; }
    public long getCooldownDuration() { return cooldownDuration; }
    public boolean isGlobalCooldown() { return globalCooldown; }
    public SurvivalModeConfig getSurvivalMode() { return survivalMode; }
    public DeathHandlingConfig getDeathHandling() { return deathHandling; }
    public ItemHandlingConfig getItemHandling() { return itemHandling; }
    public RewardConfig getRewardConfig() { return rewardConfig; }
    public String getWorldGuardRegion() { return worldGuardRegion; }
    public boolean hasWorldGuardRegion() { return worldGuardRegion != null; }

    /**
     * Configuración del modo survival
     */
    public static class SurvivalModeConfig {
        private boolean enabled;
        private boolean saveInventory;
        private boolean clearInventory;
        private boolean forceGameMode;
        private GameMode gameMode;
        private boolean allowPvP;
        
        public SurvivalModeConfig() {
            this.enabled = false;
            this.saveInventory = true;
            this.clearInventory = true;
            this.forceGameMode = true;
            this.gameMode = GameMode.SURVIVAL;
            this.allowPvP = false;
        }
        
        public static SurvivalModeConfig load(ConfigurationSection section) {
            SurvivalModeConfig config = new SurvivalModeConfig();
            
            if (section == null) return config;
            
            config.enabled = section.getBoolean("enabled", false);
            config.saveInventory = section.getBoolean("save-inventory", true);
            config.clearInventory = section.getBoolean("clear-inventory", true);
            config.forceGameMode = section.getBoolean("force-gamemode", true);
            config.allowPvP = section.getBoolean("allow-pvp", false);
            
            String gmStr = section.getString("gamemode", "SURVIVAL");
            try {
                config.gameMode = GameMode.valueOf(gmStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                config.gameMode = GameMode.SURVIVAL;
            }
            
            return config;
        }
        
        // Getters
        public boolean isEnabled() { return enabled; }
        public boolean shouldSaveInventory() { return saveInventory; }
        public boolean shouldClearInventory() { return clearInventory; }
        public boolean shouldForceGameMode() { return forceGameMode; }
        public GameMode getGameMode() { return gameMode; }
        public boolean isPvPAllowed() { return allowPvP; }
    }

    /**
     * Configuración de manejo de muerte
     */
    public static class DeathHandlingConfig {
        private DeathAction action;
        private boolean canRejoin;
        private boolean spectateOnDeath;
        private boolean teleportOnDeath;
        private int rejoinCooldown; // segundos
        
        public DeathHandlingConfig() {
            this.action = DeathAction.KICK;
            this.canRejoin = false;
            this.spectateOnDeath = true;
            this.teleportOnDeath = true;
            this.rejoinCooldown = 0;
        }
        
        public static DeathHandlingConfig load(ConfigurationSection section) {
            DeathHandlingConfig config = new DeathHandlingConfig();
            
            if (section == null) return config;
            
            String actionStr = section.getString("action", "KICK");
            try {
                config.action = DeathAction.valueOf(actionStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                config.action = DeathAction.KICK;
            }
            
            config.canRejoin = section.getBoolean("can-rejoin", false);
            config.spectateOnDeath = section.getBoolean("spectate-on-death", true);
            config.teleportOnDeath = section.getBoolean("teleport-on-death", true);
            config.rejoinCooldown = section.getInt("rejoin-cooldown", 0);
            
            return config;
        }
        
        // Getters
        public DeathAction getAction() { return action; }
        public boolean canRejoin() { return canRejoin; }
        public boolean shouldSpectate() { return spectateOnDeath; }
        public boolean shouldTeleport() { return teleportOnDeath; }
        public int getRejoinCooldown() { return rejoinCooldown; }
    }

    /**
     * Configuración de manejo de items
     */
    public static class ItemHandlingConfig {
        private boolean keepInventoryOnDeath;
        private boolean dropItemsOnDeath;
        private ItemDropMode dropMode;
        
        public ItemHandlingConfig() {
            this.keepInventoryOnDeath = true;
            this.dropItemsOnDeath = false;
            this.dropMode = ItemDropMode.ALL_PLAYERS;
        }
        
        public static ItemHandlingConfig load(ConfigurationSection section) {
            ItemHandlingConfig config = new ItemHandlingConfig();
            
            if (section == null) return config;
            
            config.keepInventoryOnDeath = section.getBoolean("keep-inventory-on-death", true);
            config.dropItemsOnDeath = section.getBoolean("drop-items-on-death", false);
            
            String dropModeStr = section.getString("drop-mode", "ALL_PLAYERS");
            try {
                config.dropMode = ItemDropMode.valueOf(dropModeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                config.dropMode = ItemDropMode.ALL_PLAYERS;
            }
            
            return config;
        }
        
        // Getters
        public boolean shouldKeepInventory() { return keepInventoryOnDeath; }
        public boolean shouldDropItems() { return dropItemsOnDeath; }
        public ItemDropMode getDropMode() { return dropMode; }
    }

    /**
     * Configuración de recompensas
     */
    public static class RewardConfig {
        private boolean enabled;
        private RewardType type;
        private double money;
        private List<String> items;
        private List<String> commands;
        private double progressiveMultiplier; // Por cada wave completada
        
        public RewardConfig() {
            this.enabled = true;
            this.type = RewardType.COMPLETION_ONLY;
            this.money = 0;
            this.items = new ArrayList<>();
            this.commands = new ArrayList<>();
            this.progressiveMultiplier = 0.1;
        }
        
        public static RewardConfig load(ConfigurationSection section) {
            RewardConfig config = new RewardConfig();
            
            if (section == null) return config;
            
            config.enabled = section.getBoolean("enabled", true);
            config.money = section.getDouble("money", 0);
            config.items = section.getStringList("items");
            config.commands = section.getStringList("commands");
            config.progressiveMultiplier = section.getDouble("progressive-multiplier", 0.1);
            
            String typeStr = section.getString("type", "COMPLETION_ONLY");
            try {
                config.type = RewardType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                config.type = RewardType.COMPLETION_ONLY;
            }
            
            return config;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        // Getters
        public boolean isEnabled() { return enabled; }
        public RewardType getType() { return type; }
        public double getMoney() { return money; }
        public List<String> getItems() { return new ArrayList<>(items); }
        public List<String> getCommands() { return new ArrayList<>(commands); }
        public double getProgressiveMultiplier() { return progressiveMultiplier; }
    }
}