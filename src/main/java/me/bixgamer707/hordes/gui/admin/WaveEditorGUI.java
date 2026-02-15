package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Wave editor GUI - 100% configurable from guis.yml
 */
public class WaveEditorGUI extends BaseGUI {

    private final Arena arena;
    private final String arenaId;
    private final int wavesPerPage;
    private final int[] waveSlots;

    public WaveEditorGUI(Hordes plugin, Player player, Arena arena) {
        super(plugin, player, "admin-wave-editor");
        this.arena = arena;
        this.arenaId = arena.getId();
        
        this.wavesPerPage = guiConfig.getInt("guis."+guiId+".waves-per-page", 28);
        this.waveSlots = parseSlots(guiConfig.getString("guis."+guiId+".wave-slots", "10-16,19-25,28-34,37-43"));
        
        int totalWaves = arena.getConfig().getTotalWaves();
        this.maxPages = (int) Math.ceil((double) totalWaves / wavesPerPage);
        if (maxPages == 0) maxPages = 1;
    }

    @Override
    protected void buildDynamic() {
        int totalWaves = arena.getConfig().getTotalWaves();
        int startWave = currentPage * wavesPerPage;
        int endWave = Math.min(startWave + wavesPerPage, totalWaves);
        
        int slotIndex = 0;
        for (int wave = startWave; wave < endWave && slotIndex < waveSlots.length; wave++) {
            int slot = waveSlots[slotIndex];
            int waveNumber = wave + 1; // Waves are 1-indexed
            
            ItemStack waveItem = createWaveItem(waveNumber);
            inventory.setItem(slot, waveItem);
            
            final int finalWave = waveNumber;
            clickHandlers.put(slot + "", p -> handleWaveClick(p, finalWave));
            
            slotIndex++;
        }
        
        updatePaginationButtons();
    }

    /**
     * Parse slots from config
     */
    private int[] parseSlots(String slotsConfig) {
        List<Integer> slots = new ArrayList<>();
        for (String part : slotsConfig.split(",")) {
            if (part.contains("-")) {
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                for (int i = start; i <= end; i++) {
                    slots.add(i);
                }
            } else {
                slots.add(Integer.parseInt(part.trim()));
            }
        }
        return slots.stream().mapToInt(i -> i).toArray();
    }

    /**
     * Creates wave item from config
     */
    private ItemStack createWaveItem(int waveNumber) {
        // Get wave config from mobs.yml
        boolean hasConfig = plugin.getFileManager().getMobs()
            .contains(arenaId + ".wave-" + waveNumber);
        
        // Get material from GUI config
        String materialKey = "items.wave-item.material-" + (hasConfig ? "configured" : "not-configured");
        Material material = Material.valueOf(guiConfig.getString("guis."+guiId+"."+materialKey, "GREEN_WOOL").toUpperCase());
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Get name from config
            String name = guiConfig.getString("guis."+guiId+".items.wave-item.name", "&e&lWave {wave_number}")
                .replace("{wave_number}", String.valueOf(waveNumber));
            meta.setDisplayName(Text.createText(name).build());
            
            // Get lore from config
            List<String> loreTemplate = guiConfig.getStringList("guis."+guiId+".items.wave-item.lore");
            List<String> lore = new ArrayList<>();
            
            // Get wave info if exists
            String mobsInfo = "Not configured";
            if (hasConfig) {
                // Get mob count or details
                mobsInfo = getMobsInfo(waveNumber);
            }
            
            for (String line : loreTemplate) {
                String processed = line
                    .replace("{wave_number}", String.valueOf(waveNumber))
                    .replace("{status}", hasConfig ?
                            guiConfig.getString("guis."+guiId+".placeholders.wave-configured", "&a✔ Configured") :
                            guiConfig.getString("guis."+guiId+".placeholders.wave-not-configured", "&c✘ Not configured"))
                    .replace("{mobs_info}", mobsInfo);
                
                lore.add(Text.createText(processed).build(player));
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Gets mobs info for wave
     */
    private String getMobsInfo(int waveNumber) {
        // Get from mobs.yml
        var mobsConfig = plugin.getFileManager().getMobs()
            .getConfigurationSection(arenaId + ".wave-" + waveNumber + ".mobs");
        
        if (mobsConfig == null) {
            return "No mobs";
        }
        
        int totalMobs = 0;
        for (String key : mobsConfig.getKeys(false)) {
            totalMobs += mobsConfig.getInt(key + ".amount", 0);
        }
        
        return totalMobs + " mobs";
    }

    /**
     * Handles wave click
     */
    private void handleWaveClick(Player player, int waveNumber) {
        player.sendMessage(Text.createTextWithLang("admin.wave-edit-coming-soon")
                .replace("{0}", String.valueOf(waveNumber))
                .build());
    }

    /**
     * Updates pagination buttons
     */
    private void updatePaginationButtons() {
        int prevSlot = guiConfig.getInt("guis."+guiId+".items.previous-page.slot", 45);
        int nextSlot = guiConfig.getInt("guis."+guiId+".items.next-page.slot", 53);
        
        if (currentPage > 0) {
            updateItemLore(prevSlot, new String[]{
                String.valueOf(currentPage + 1),
                String.valueOf(maxPages)
            });
        } else {
            inventory.setItem(prevSlot, null);
        }
        
        if (currentPage < maxPages - 1) {
            updateItemLore(nextSlot, new String[]{
                String.valueOf(currentPage + 1),
                String.valueOf(maxPages)
            });
        } else {
            inventory.setItem(nextSlot, null);
        }
    }

    @Override
    protected void handleCustomAction(int slot, String actionType, String actionValue, String itemId) {
        if (actionType.equals("add-wave")) {
            // Add new wave
            int newWave = arena.getConfig().getTotalWaves() + 1;
            plugin.getFileManager().getFile("arenas.yml")
                .set("arenas." + arenaId + ".total-waves", newWave);
            plugin.getFileManager().getArenas().save();

            player.sendMessage(Text.createTextWithLang("admin.wave-added")
                            .replace("{count}", String.valueOf(newWave))
                    .build());
            
            plugin.getArenaManager().loadArenas();
            refresh();
        }
    }

    @Override
    protected void onBack() {
        new ArenaEditorGUI(plugin, player, arena).open();
    }
}
