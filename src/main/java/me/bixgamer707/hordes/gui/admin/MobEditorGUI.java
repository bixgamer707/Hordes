package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.mob.MobType;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Material;
import org.bukkit.conversations.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mob editor GUI for specific wave
 * 100% configurable - supports both MythicMobs and vanilla
 */
public class MobEditorGUI extends BaseGUI {

    private final Arena arena;
    private final String arenaId;
    private final int waveNumber;
    private List<MobEntry> mobs;

    public MobEditorGUI(Hordes plugin, Player player, Arena arena, int waveNumber) {
        super(plugin, player, "admin-mob-editor");
        this.arena = arena;
        this.arenaId = arena.getId();
        this.waveNumber = waveNumber;
        this.mobs = new ArrayList<>();
        
        loadMobs();
    }

    /**
     * Load mobs from mobs.yml
     */
    private void loadMobs() {
        mobs.clear();
        
        String basePath = arenaId + ".wave-" + waveNumber + ".mobs";
        List<Map<?, ?>> mobList = plugin.getFileManager().getMobs().getMapList(basePath);
        
        if (mobList == null) return;
        
        for (Map<?, ?> mobMap : mobList) {
            MobEntry entry = new MobEntry();
            entry.type = MobType.valueOf(String.valueOf(mobMap.getOrDefault("type", "VANILLA")).toUpperCase());
            entry.id = String.valueOf(mobMap.get("id"));
            entry.amount = ((Number) mobMap.getOrDefault("amount", 1)).intValue();
            entry.healthMultiplier = ((Number) mobMap.getOrDefault("health-multiplier", 1.0)).doubleValue();
            entry.damageMultiplier = ((Number) mobMap.getOrDefault("damage-multiplier", 1.0)).doubleValue();
            entry.customName = (String) mobMap.get("custom-name");
            mobs.add(entry);
        }
    }

    @Override
    protected void buildDynamic() {
        updateWaveInfo();
        updateSpawnSettings();
        displayMobs();
    }

    /**
     * Update wave information header
     */
    private void updateWaveInfo() {
        int slot = getConfigInt("items.wave-info.slot", 4);
        
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = getConfigString("items.wave-info.name", "&e&lWave {wave}")
                .replace("{wave}", String.valueOf(waveNumber));
            meta.setDisplayName(Text.createText(name).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : getConfigStringList("items.wave-info.lore")) {
                lore.add(Text.createText(line
                    .replace("{wave}", String.valueOf(waveNumber))
                    .replace("{total_mobs}", String.valueOf(mobs.size()))
                    .replace("{total_count}", String.valueOf(getTotalMobCount())))
                    .build(player));
            }
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
    }

    /**
     * Update spawn settings display
     */
    private void updateSpawnSettings() {
        int slot = getConfigInt("items.spawn-settings.slot", 10);
        
        String basePath = arenaId + ".wave-" + waveNumber;
        int spawnDelay = plugin.getFileManager().getMobs().getInt(basePath + ".spawn-delay", 20);
        int mobsPerSpawn = plugin.getFileManager().getMobs().getInt(basePath + ".mobs-per-spawn", 1);
        
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = getConfigString("items.spawn-settings.name", "&6&lSpawn Settings");
            meta.setDisplayName(Text.createText(name).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : getConfigStringList("items.spawn-settings.lore")) {
                lore.add(Text.createText(line
                    .replace("{spawn_delay}", String.valueOf(spawnDelay))
                    .replace("{mobs_per_spawn}", String.valueOf(mobsPerSpawn))
                    .replace("{delay_seconds}", String.format("%.1f", spawnDelay / 20.0)))
                    .build(player));
            }
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        inventory.setItem(slot, item);
        clickHandlers.put(slot + "", p -> editSpawnSettings());
    }

    /**
     * Display mob list
     */
    private void displayMobs() {
        // Get display slots from config
        int[] slots = parseSlots(getConfigString("mob-display-slots", "19-25,28-34"));
        
        for (int i = 0; i < Math.min(mobs.size(), slots.length); i++) {
            int slot = slots[i];
            MobEntry mob = mobs.get(i);
            
            ItemStack item = createMobItem(mob, i);
            inventory.setItem(slot, item);
            
            final int index = i;
            clickHandlers.put(slot + "", p -> editMob(index));
        }
    }

    /**
     * Create item for mob display
     */
    private ItemStack createMobItem(MobEntry mob, int index) {
        Material material;
        
        if (mob.type == MobType.MYTHIC) {
            material = Material.valueOf(getConfigString("items.mob-item.material-mythic", "SPAWNER"));
        } else {
            material = Material.valueOf(getConfigString("items.mob-item.material-vanilla", "ZOMBIE_HEAD"));
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = getConfigString("items.mob-item.name", "&e{mob_id}")
                .replace("{mob_id}", mob.id)
                .replace("{index}", String.valueOf(index + 1));
            meta.setDisplayName(Text.createText(name).build(player));
            
            List<String> lore = new ArrayList<>();
            for (String line : getConfigStringList("items.mob-item.lore")) {
                lore.add(Text.createText(line
                    .replace("{type}", mob.type.getDisplayName())
                    .replace("{mob_id}", mob.id)
                    .replace("{amount}", String.valueOf(mob.amount))
                    .replace("{health}", String.format("%.1f", mob.healthMultiplier))
                    .replace("{damage}", String.format("%.1f", mob.damageMultiplier))
                    .replace("{custom_name}", mob.customName != null ? mob.customName : "&7None"))
                    .build(player));
            }
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Parse slot ranges
     */
    private int[] parseSlots(String config) {
        List<Integer> slots = new ArrayList<>();
        for (String part : config.split(",")) {
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
     * Get total mob count
     */
    private int getTotalMobCount() {
        return mobs.stream().mapToInt(m -> m.amount).sum();
    }

    /**
     * Edit spawn settings
     */
    private void editSpawnSettings() {
        close();
        
        sendConfigMessage("mobs.edit-spawn-header");
        
        ConversationFactory factory = new ConversationFactory(plugin)
            .withFirstPrompt(new NumericPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return Text.createText(getConfigString("prompts.spawn-delay", 
                        "Enter spawn delay in ticks (20 = 1 second):")).build();
                }
                
                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                    int delay = input.intValue();
                    
                    if (delay < 1 || delay > 200) {
                        sendConfigMessage("mobs.invalid-spawn-delay");
                        return this;
                    }
                    
                    context.setSessionData("spawn-delay", delay);
                    return new NumericPrompt() {
                        @Override
                        public String getPromptText(ConversationContext context) {
                            return Text.createText(getConfigString("prompts.mobs-per-spawn", 
                                "Enter mobs per spawn cycle:")).build();
                        }
                        
                        @Override
                        protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                            int mobsPerSpawn = input.intValue();
                            
                            if (mobsPerSpawn < 1 || mobsPerSpawn > 10) {
                                sendConfigMessage("mobs.invalid-mobs-per-spawn");
                                return this;
                            }
                            
                            // Save both values
                            String basePath = arenaId + ".wave-" + waveNumber;
                            plugin.getFileManager().getMobs().set(basePath + ".spawn-delay", 
                                context.getSessionData("spawn-delay"));
                            plugin.getFileManager().getMobs().set(basePath + ".mobs-per-spawn", mobsPerSpawn);
                            plugin.getFileManager().getMobs().save();
                            
                            sendConfigMessage("mobs.spawn-settings-updated");
                            
                            return Prompt.END_OF_CONVERSATION;
                        }
                    };
                }
            })
            .withLocalEcho(false)
            .withTimeout(getConfigInt("prompts.timeout", 60))
            .addConversationAbandonedListener(event -> reopenGUI())
            .buildConversation(player);
        
        factory.begin();
    }

    /**
     * Add new mob
     */
    private void addMob() {
        close();
        
        sendConfigMessage("mobs.add-mob-header");
        sendConfigMessage("mobs.add-mob-help");
        
        ConversationFactory factory = new ConversationFactory(plugin)
            .withFirstPrompt(new StringPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return Text.createText(getConfigString("prompts.mob-type", 
                        "Enter mob type (VANILLA or MYTHIC):")).build();
                }
                
                @Override
                public Prompt acceptInput(ConversationContext context, String input) {
                    MobType type;
                    try {
                        type = MobType.valueOf(input.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        sendConfigMessage("mobs.invalid-mob-type");
                        return this;
                    }
                    
                    context.setSessionData("mob-type", type);
                    
                    return new StringPrompt() {
                        @Override
                        public String getPromptText(ConversationContext context) {
                            MobType mobType = (MobType) context.getSessionData("mob-type");
                            if (mobType == MobType.VANILLA) {
                                return Text.createText(getConfigString("prompts.mob-id-vanilla", 
                                    "Enter EntityType (ZOMBIE, SKELETON, etc.):")).build();
                            } else {
                                return Text.createText(getConfigString("prompts.mob-id-mythic", 
                                    "Enter MythicMobs ID:")).build();
                            }
                        }
                        
                        @Override
                        public Prompt acceptInput(ConversationContext context, String input) {
                            MobType mobType = (MobType) context.getSessionData("mob-type");
                            
                            // Validate vanilla entity type
                            if (mobType == MobType.VANILLA) {
                                try {
                                    EntityType.valueOf(input.toUpperCase());
                                } catch (IllegalArgumentException e) {
                                    sendConfigMessage("mobs.invalid-entity-type", input);
                                    return this;
                                }
                            }
                            
                            context.setSessionData("mob-id", input.toUpperCase());
                            
                            return new NumericPrompt() {
                                @Override
                                public String getPromptText(ConversationContext context) {
                                    return Text.createText(getConfigString("prompts.mob-amount", 
                                        "Enter amount to spawn:")).build();
                                }
                                
                                @Override
                                protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                                    int amount = input.intValue();
                                    
                                    if (amount < 1 || amount > 100) {
                                        sendConfigMessage("mobs.invalid-amount");
                                        return this;
                                    }
                                    
                                    // Create mob entry
                                    MobEntry mob = new MobEntry();
                                    mob.type = (MobType) context.getSessionData("mob-type");
                                    mob.id = (String) context.getSessionData("mob-id");
                                    mob.amount = amount;
                                    mob.healthMultiplier = 1.0;
                                    mob.damageMultiplier = 1.0;
                                    
                                    // Save to file
                                    saveMobToFile(mob);
                                    
                                    sendConfigMessage("mobs.mob-added", mob.id, amount);
                                    
                                    return Prompt.END_OF_CONVERSATION;
                                }
                            };
                        }
                    };
                }
            })
            .withLocalEcho(false)
            .withTimeout(getConfigInt("prompts.timeout", 120))
            .addConversationAbandonedListener(event -> reopenGUI())
            .buildConversation(player);
        
        factory.begin();
    }

    /**
     * Edit existing mob
     */
    private void editMob(int index) {
        if (index < 0 || index >= mobs.size()) return;
        
        MobEntry mob = mobs.get(index);
        close();
        
        sendConfigMessage("mobs.edit-mob-header", mob.id);
        sendConfigMessage("mobs.edit-mob-options");
        
        // For now, just allow editing multipliers
        ConversationFactory factory = new ConversationFactory(plugin)
            .withFirstPrompt(new NumericPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return Text.createText(getConfigString("prompts.health-multiplier", 
                        "Enter health multiplier (1.0 = normal):")).build();
                }
                
                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                    double health = input.doubleValue();
                    
                    if (health <= 0 || health > 10) {
                        sendConfigMessage("mobs.invalid-multiplier");
                        return this;
                    }
                    
                    mob.healthMultiplier = health;
                    
                    return new NumericPrompt() {
                        @Override
                        public String getPromptText(ConversationContext context) {
                            return Text.createText(getConfigString("prompts.damage-multiplier", 
                                "Enter damage multiplier (1.0 = normal):")).build();
                        }
                        
                        @Override
                        protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                            double damage = input.doubleValue();
                            
                            if (damage <= 0 || damage > 10) {
                                sendConfigMessage("mobs.invalid-multiplier");
                                return this;
                            }
                            
                            mob.damageMultiplier = damage;
                            
                            // Save updated mob
                            saveMobsToFile();
                            
                            sendConfigMessage("mobs.mob-updated", mob.id);
                            
                            return Prompt.END_OF_CONVERSATION;
                        }
                    };
                }
            })
            .withLocalEcho(false)
            .withTimeout(getConfigInt("prompts.timeout", 60))
            .addConversationAbandonedListener(event -> reopenGUI())
            .buildConversation(player);
        
        factory.begin();
    }

    /**
     * Save single mob to file
     */
    private void saveMobToFile(MobEntry mob) {
        mobs.add(mob);
        saveMobsToFile();
    }

    /**
     * Save all mobs to file
     */
    private void saveMobsToFile() {
        List<Map<String, Object>> mobList = new ArrayList<>();
        
        for (MobEntry mob : mobs) {
            Map<String, Object> mobMap = new HashMap<>();
            mobMap.put("type", mob.type.name());
            mobMap.put("id", mob.id);
            mobMap.put("amount", mob.amount);
            mobMap.put("health-multiplier", mob.healthMultiplier);
            mobMap.put("damage-multiplier", mob.damageMultiplier);
            if (mob.customName != null) {
                mobMap.put("custom-name", mob.customName);
            }
            mobList.add(mobMap);
        }
        
        String basePath = arenaId + ".wave-" + waveNumber + ".mobs";
        plugin.getFileManager().getMobs().set(basePath, mobList);
        plugin.getFileManager().getMobs().save();
    }

    /**
     * Reopen GUI
     */
    private void reopenGUI() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            new MobEditorGUI(plugin, player, arena, waveNumber).open();
        }, 1L);
    }

    @Override
    protected void handleCustomAction(String actionType, String actionValue, String itemId) {
        switch (actionType) {
            case "add-mob":
                addMob();
                break;
                
            case "clear-mobs":
                String basePath = arenaId + ".wave-" + waveNumber + ".mobs";
                plugin.getFileManager().getMobs().set(basePath, new ArrayList<>());
                plugin.getFileManager().getMobs().save();
                sendConfigMessage("mobs.all-mobs-cleared");
                playSound("success");
                reopenGUI();
                break;
        }
    }

    @Override
    protected void onBack() {
        new WaveEditorGUI(plugin, player, arena).open();
    }

    /**
     * Mob entry class
     */
    private static class MobEntry {
        MobType type;
        String id;
        int amount;
        double healthMultiplier;
        double damageMultiplier;
        String customName;
    }
}