package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.config.SpawnConfigManager;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Spawn editor GUI - 100% configurable
 */
public class SpawnEditorGUI extends BaseGUI {

    private final Arena arena;
    private final String arenaId;

    public SpawnEditorGUI(Hordes plugin, Player player, Arena arena) {
        super(plugin, player, "admin-spawn-editor");
        this.arena = arena;
        this.arenaId = arena.getId();
    }

    @Override
    protected void buildDynamic() {
        // Get configured slots for each spawn type
        int lobbySlot = getConfigInt("spawn-slots.lobby", 11);
        int arenaSlot = getConfigInt("spawn-slots.arena", 13);
        int exitSlot = getConfigInt("spawn-slots.exit", 15);

        createSpawnItem("lobby", lobbySlot);
        createSpawnItem("arena", arenaSlot);
        createSpawnItem("exit", exitSlot);
    }

    /**
     * Creates spawn item with current status from config
     */
    private void createSpawnItem(String spawnType, int slot) {
        Location spawn = getSpawnLocation(spawnType);
        boolean hasSpawn = spawn != null;

        // Get materials from config
        String materialKey = "spawn-item.material-" + (hasSpawn ? "set" : "not-set");
        String materialName = getConfigString(materialKey, hasSpawn ? "GREEN_WOOL" : "RED_WOOL");
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (Exception e) {
            material = Material.GRAY_WOOL;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Get name from config
            String nameTemplate = getConfigString("spawn-item.name", "&e{spawn_type} Spawn");
            String typeName = spawnType.substring(0, 1).toUpperCase() + spawnType.substring(1);
            String name = nameTemplate.replace("{spawn_type}", typeName);
            meta.setDisplayName(Text.createText(name).build(player));

            // Get lore from config
            List<String> loreTemplate;
            if (hasSpawn) {
                loreTemplate = guiConfig.getStringList("guis." + guiId + ".spawn-item.lore-set");
            } else {
                loreTemplate = guiConfig.getStringList("guis." + guiId + ".spawn-item.lore-not-set");
            }

            List<String> lore = new ArrayList<>();
            for (String line : loreTemplate) {
                String processed = line;
                if (hasSpawn) {
                    processed = processed
                            .replace("{world}", spawn.getWorld().getName())
                            .replace("{x}", String.valueOf(spawn.getBlockX()))
                            .replace("{y}", String.valueOf(spawn.getBlockY()))
                            .replace("{z}", String.valueOf(spawn.getBlockZ()));
                }
                lore.add(Text.createText(processed).build(player));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> handleSpawnClick(p, spawnType));
    }

    /**
     * Gets spawn location from arena
     */
    private Location getSpawnLocation(String spawnType) {
        switch (spawnType) {
            case "lobby":
                return arena.getConfig().getLobbySpawn();
            case "arena":
                return arena.getConfig().getArenaSpawn();
            case "exit":
                return arena.getConfig().getExitLocation();
            default:
                return null;
        }
    }

    /**
     * Handles spawn click actions
     */
    private void handleSpawnClick(Player player, String spawnType) {
        // For now, default to set spawn (left-click)
        // Click types would be handled by GUIListener
        Location playerLoc = player.getLocation();
        SpawnConfigManager spawnManager = new SpawnConfigManager(plugin);

        boolean success = spawnManager.setSpawn(arenaId, spawnType, playerLoc);

        if (success) {
            sendConfigMessage("admin.spawn-set-success",
                    spawnType.substring(0,1).toUpperCase() + spawnType.substring(1));
            playSound("success");

            plugin.getArenaManager().loadArenas();
            refresh();
        } else {
            sendConfigMessage("admin.spawn-set-failed");
            playSound("error");
        }
    }

    @Override
    protected void handleCustomAction(String actionType, String actionValue, String itemId) {
        // Custom actions for TP, clear, etc. from config
    }

    @Override
    protected void onBack() {
        new ArenaEditorGUI(plugin, player, arena).open();
    }
}