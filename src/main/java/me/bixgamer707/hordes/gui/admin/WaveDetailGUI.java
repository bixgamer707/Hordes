package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import me.bixgamer707.hordes.utils.InputValidators;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Editor for a single wave: spawn-delay, mobs-per-spawn, per-wave progression
 * override, and full CRUD over that wave's mob list (mobs.yml).
 * <p>
 * Fully rebuilds its own dynamic content on every open/refresh - matches the
 * self-contained pattern already used by RewardEditorGUI/ModeSettingsGUI.
 */
public class WaveDetailGUI extends BaseGUI {

    private static final int[] MOB_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    private final Arena arena;
    private final String arenaId;
    private final int waveNumber;
    private final String wavePath;

    public WaveDetailGUI(Hordes plugin, Player player, Arena arena, int waveNumber) {
        super(plugin, player, "admin-wave-detail", mapOf(arena.getId(), waveNumber));
        this.arena = arena;
        this.arenaId = arena.getId();
        this.waveNumber = waveNumber;
        this.wavePath = arenaId + ".wave-" + waveNumber;

        ensureWaveSectionExists();
    }

    private static Map<String, String> mapOf(String arenaId, int waveNumber) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("arena_id", arenaId);
        map.put("wave_number", String.valueOf(waveNumber));
        return map;
    }

    /**
     * If this wave has no config yet (e.g. was just added via "Add Wave"),
     * give it sensible defaults so it's immediately usable.
     */
    private void ensureWaveSectionExists() {
        if (!plugin.getFileManager().getMobs().contains(wavePath)) {
            plugin.getFileManager().getMobs().set(wavePath + ".spawn-delay", 20);
            plugin.getFileManager().getMobs().set(wavePath + ".mobs-per-spawn", 1);
            plugin.getFileManager().getMobs().set(wavePath + ".mobs", new ArrayList<Map<String, Object>>());
            plugin.getFileManager().getMobs().save();
        }
    }

    @Override
    protected void buildDynamic() {
        buildMobGrid();
        buildItems();
    }

    private void buildItems() {
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("guis." + guiId + ".items");
        if (itemsSection == null) {
            return;
        }

        for (String itemId : itemsSection.getKeys(false)) {
            if ("add-mythic".equals(itemId) && !isMythicAvailable()) {
                continue;
            }

            String itemPath = "guis." + guiId + ".items." + itemId;
            if ("border".equals(itemId) || "back".equals(itemId)) {
                loadAndSetItem(itemId, itemPath);
                continue;
            }

            setConfiguredWaveItem(itemId, itemPath);
        }
    }

    private boolean isMythicAvailable() {
        return plugin.getMythicMobsIntegration() != null
                && plugin.getMythicMobsIntegration().isEnabled();
    }

    private void setConfiguredWaveItem(String itemId, String itemPath) {
        List<Integer> slots = new ArrayList<>();
        Object slotObj = guiConfig.get(itemPath + ".slot");

        if (slotObj instanceof Integer) {
            slots.add((Integer) slotObj);
        } else if (slotObj instanceof List) {
            for (Object obj : (List<?>) slotObj) {
                if (obj instanceof Integer) {
                    slots.add((Integer) obj);
                }
            }
        } else if (slotObj instanceof String) {
            slots.addAll(parseSlotString((String) slotObj));
        }

        if (slots.isEmpty()) {
            return;
        }

        ItemStack item = createConfiguredWaveItem(itemId, itemPath);
        if (item == null) {
            return;
        }

        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item);

                final List<String> action = getConfiguredActions(itemPath);
                if (!action.isEmpty()) {
                    clickHandlers.put(slot + "", p -> handleAction(slot, action, itemId, p));
                }
            }
        }
    }

    private List<String> getConfiguredActions(String itemPath) {
        List<String> actions = guiConfig.getStringList(itemPath + ".action");
        if (actions.isEmpty()) {
            String singleAction = guiConfig.getString(itemPath + ".action");
            if (singleAction != null && !singleAction.isEmpty()) {
                return Collections.singletonList(singleAction);
            }
        }
        return actions;
    }

    private ItemStack createConfiguredWaveItem(String itemId, String itemPath) {
        String materialName = guiConfig.getString(itemPath + ".material");

        if ("progression".equals(itemId)) {
            materialName = isManualWaveProgression()
                    ? guiConfig.getString(itemPath + ".material-manual", "LEVER")
                    : guiConfig.getString(itemPath + ".material-auto", "REPEATER");
        }

        if (materialName == null) {
            return null;
        }

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) {
            plugin.logWarning("Invalid material in guis.yml for " + itemId + ": " + materialName);
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String name = guiConfig.getString(itemPath + ".name");
        if (name != null) {
            meta.setDisplayName(Text.createText(applyDynamicPlaceholders(name)).build(player));
        }

        List<String> loreTemplate = guiConfig.getStringList(itemPath + ".lore");
        if (!loreTemplate.isEmpty()) {
            List<String> lore = new ArrayList<>();
            for (String line : loreTemplate) {
                lore.add(Text.createText(applyDynamicPlaceholders(line)).build(player));
            }
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private String applyDynamicPlaceholders(String text) {
        if (text == null) {
            return "";
        }

        int spawnDelay = plugin.getFileManager().getMobs().getInt(wavePath + ".spawn-delay", 20);
        int mobsPerSpawn = plugin.getFileManager().getMobs().getInt(wavePath + ".mobs-per-spawn", 1);
        String progression = isManualWaveProgression() ? "MANUAL" : "AUTO";
        List<Map<?, ?>> mobs = loadMobsList();
        int mobsCount = mobs.size();
        int totalMobs = 0;
        for (Map<?, ?> mob : mobs) {
            Object amount = mob.get("amount");
            if (amount instanceof Number) {
                totalMobs += ((Number) amount).intValue();
            } else {
                totalMobs += 1;
            }
        }

        return text
                .replace("{arena_id}", arenaId)
                .replace("{arena_name}", arena.getConfig().getDisplayName())
                .replace("{wave_number}", String.valueOf(waveNumber))
                .replace("{total_waves}", String.valueOf(arena.getConfig().getTotalWaves()))
                .replace("{spawn_delay}", String.valueOf(spawnDelay))
                .replace("{mobs_per_spawn}", String.valueOf(mobsPerSpawn))
                .replace("{progression}", progression)
                .replace("{mobs_count}", String.valueOf(mobsCount))
                .replace("{total_mobs}", String.valueOf(totalMobs));
    }

    private boolean isManualWaveProgression() {
        return "MANUAL".equalsIgnoreCase(
                plugin.getFileManager().getMobs().getString(wavePath + ".progression", "AUTO"));
    }

    // ==========================================
    // TOP CONTROL ROW
    // ==========================================

    private void editSpawnDelay() {
        close();
        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.spawn-delay").build())
                .withValidator(InputValidators.positiveInteger())
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-number").build())
                .onComplete(input -> {
                    int value = Integer.parseInt(input.trim());
                    plugin.getFileManager().getMobs().set(wavePath + ".spawn-delay", value);
                    plugin.getFileManager().getMobs().save();
                    player.sendMessage(Text.createTextWithLang("prompts.spawn-delay-updated")
                            .replace("{0}", String.valueOf(value)).build(player));
                    open();
                })
                .onCancel(this::open)
                .start();
    }

    private void editMobsPerSpawn() {
        close();
        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.mobs-per-spawn").build())
                .withValidator(InputValidators.positiveInteger())
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-number").build())
                .onComplete(input -> {
                    int value = Integer.parseInt(input.trim());
                    plugin.getFileManager().getMobs().set(wavePath + ".mobs-per-spawn", value);
                    plugin.getFileManager().getMobs().save();
                    player.sendMessage(Text.createTextWithLang("prompts.mobs-per-spawn-updated")
                            .replace("{0}", String.valueOf(value)).build(player));
                    open();
                })
                .onCancel(this::open)
                .start();
    }

    // ==========================================
    // MOB GRID
    // ==========================================

    @SuppressWarnings("unchecked")
    private void buildMobGrid() {
        List<Map<?, ?>> mobs = loadMobsList();

        for (int i = 0; i < MOB_SLOTS.length; i++) {
            int slot = MOB_SLOTS[i];

            if (i >= mobs.size()) {
                inventory.setItem(slot, null);
                continue;
            }

            Map<String, Object> mob = (Map<String, Object>) mobs.get(i);
            inventory.setItem(slot, createMobItem(mob));

            final int mobIndex = i;
            clickHandlers.put(slot + "", p -> handleMobClick(mobIndex));
        }
    }

    private ItemStack createMobItem(Map<String, Object> mob) {
        String type = String.valueOf(mob.getOrDefault("type", "VANILLA"));
        String id = String.valueOf(mob.getOrDefault("id", "ZOMBIE"));
        int amount = toInt(mob.get("amount"), 1);
        double healthMult = toDouble(mob.get("health-multiplier"), 1.0);
        double damageMult = toDouble(mob.get("damage-multiplier"), 1.0);

        Material material = resolveMobMaterial(type, id);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String nameTemplate = guiConfig.getString("guis." + guiId + ".items.mob-item.name", "&e&l{mob_id}");
            String displayName = replaceMobPlaceholders(nameTemplate, type, id, amount, healthMult, damageMult);
            meta.setDisplayName(Text.createText(displayName).build(player));

            List<String> loreTemplate = guiConfig.getStringList("guis." + guiId + ".items.mob-item.lore");
            List<String> lore = new ArrayList<>();
            if (loreTemplate.isEmpty()) {
                lore.add(Text.createText("&7Type: &e" + type).build(player));
                lore.add(Text.createText("&7Amount: &e" + amount).build(player));
                lore.add(Text.createText("&7Health x: &e" + healthMult).build(player));
                lore.add(Text.createText("&7Damage x: &e" + damageMult).build(player));
                lore.add("");
                lore.add(Text.createText("&eLeft-click: &7edit amount").build(player));
                lore.add(Text.createText("&eShift-click: &7edit multipliers").build(player));
                lore.add(Text.createText("&cRight-click: &7remove").build(player));
            } else {
                for (String line : loreTemplate) {
                    lore.add(Text.createText(replaceMobPlaceholders(line, type, id, amount, healthMult, damageMult)).build(player));
                }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material resolveMobMaterial(String type, String id) {
        String configPath = "guis." + guiId + ".items.mob-item";
        String materialName = guiConfig.getString(configPath + ".material");
        if (materialName == null) {
            String typeKey = "material-" + type.toLowerCase();
            materialName = guiConfig.getString(configPath + "." + typeKey);
        }

        if (materialName != null) {
            Material material = Material.getMaterial(materialName.toUpperCase());
            if (material != null) {
                return material;
            }
            plugin.logWarning("Invalid mob material in guis.yml: " + materialName + " for " + type + " mob");
        }

        return "MYTHIC".equalsIgnoreCase(type) ? Material.NETHER_STAR : vanillaSpawnEggOrFallback(id);
    }

    private String replaceMobPlaceholders(String text, String type, String id, int amount, double healthMult, double damageMult) {
        if (text == null) {
            return "";
        }
        return text
                .replace("{mob_type}", type)
                .replace("{mob_id}", id)
                .replace("{mob_amount}", String.valueOf(amount))
                .replace("{mob_health_mult}", String.valueOf(healthMult))
                .replace("{mob_damage_mult}", String.valueOf(damageMult));
    }

    /**
     * Tries to show the actual mob's spawn egg for a nicer preview; falls back
     * to a zombie head icon if the id isn't a recognized vanilla entity/there's
     * no matching spawn egg (e.g. bosses).
     */
    private Material vanillaSpawnEggOrFallback(String id) {
        try {
            Material egg = Material.valueOf(id.toUpperCase() + "_SPAWN_EGG");
            if (egg.isItem()) return egg;
        } catch (IllegalArgumentException ignored) {
            // no matching spawn egg, fall through
        }
        return Material.ZOMBIE_HEAD;
    }

    private void handleMobClick(int mobIndex) {
        ClickType clickType = clickTypes.get(MOB_SLOTS[mobIndex]);

        if (clickType == ClickType.RIGHT) {
            List<Map<?, ?>> mobs = loadMobsList();
            if (mobIndex < mobs.size()) {
                mobs.remove(mobIndex);
                saveMobsList(mobs);
                player.sendMessage(Text.createTextWithLang("prompts.mob-removed").build(player));
            }
            refresh();
        } else if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            editMobMultipliers(mobIndex);
        } else {
            editMobAmount(mobIndex);
        }
    }

    @SuppressWarnings("unchecked")
    private void editMobAmount(int mobIndex) {
        List<Map<?, ?>> mobs = loadMobsList();
        if (mobIndex >= mobs.size()) return;

        close();
        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.mob-amount").build())
                .withValidator(InputValidators.positiveInteger())
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-number").build())
                .onComplete(input -> {
                    List<Map<?, ?>> current = loadMobsList();
                    if (mobIndex < current.size()) {
                        Map<String, Object> mob = (Map<String, Object>) current.get(mobIndex);
                        mob.put("amount", Integer.parseInt(input.trim()));
                        saveMobsList(current);
                        player.sendMessage(Text.createTextWithLang("prompts.mob-amount-updated")
                                .replace("{0}", input.trim()).build(player));
                    }
                    open();
                })
                .onCancel(this::open)
                .start();
    }

    @SuppressWarnings("unchecked")
    private void editMobMultipliers(int mobIndex) {
        List<Map<?, ?>> mobs = loadMobsList();
        if (mobIndex >= mobs.size()) return;

        close();
        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.mob-health-multiplier").build())
                .withValidator(InputValidators.decimalRange(0.01, 100))
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-multiplier").build())
                .onComplete(healthInput -> {
                    double health = Double.parseDouble(healthInput.trim());

                    plugin.getChatInputManager().requestInput(player)
                            .withPrompt(Text.createTextWithLang("prompts.mob-damage-multiplier").build())
                            .withValidator(InputValidators.decimalRange(0.01, 100))
                            .withInvalidMessage(Text.createTextWithLang("prompts.invalid-multiplier").build())
                            .onComplete(damageInput -> {
                                double damage = Double.parseDouble(damageInput.trim());

                                List<Map<?, ?>> current = loadMobsList();
                                if (mobIndex < current.size()) {
                                    Map<String, Object> mob = (Map<String, Object>) current.get(mobIndex);
                                    mob.put("health-multiplier", health);
                                    mob.put("damage-multiplier", damage);
                                    saveMobsList(current);
                                    player.sendMessage(Text.createTextWithLang("prompts.mob-multipliers-updated")
                                            .build(player));
                                }
                                open();
                            })
                            .onCancel(this::open)
                            .start();
                })
                .onCancel(this::open)
                .start();
    }

    // ==========================================
    // ADD MOB BUTTONS
    // ==========================================

    private void buildAddMobButtons() {
        ItemStack vanilla = new ItemStack(Material.EMERALD);
        ItemMeta vMeta = vanilla.getItemMeta();
        if (vMeta != null) {
            vMeta.setDisplayName(Text.createText("&a&l+ Add Vanilla Mob").build(player));
            List<String> lore = new ArrayList<>();
            lore.add(Text.createText("&7Add a vanilla Minecraft mob").build(player));
            lore.add(Text.createText("&7(e.g. ZOMBIE, SKELETON, CREEPER)").build(player));
            lore.add("");
            lore.add(Text.createText("&eClick to add").build(player));
            vMeta.setLore(lore);
            vanilla.setItemMeta(vMeta);
        }
        inventory.setItem(49, vanilla);
        clickHandlers.put("49", p -> addMobFlow("VANILLA"));

        boolean mythicAvailable = plugin.getMythicMobsIntegration() != null
                && plugin.getMythicMobsIntegration().isEnabled();

        if (mythicAvailable) {
            ItemStack mythic = new ItemStack(Material.NETHER_STAR);
            ItemMeta mMeta = mythic.getItemMeta();
            if (mMeta != null) {
                mMeta.setDisplayName(Text.createText("&d&l+ Add MythicMob").build(player));
                List<String> lore = new ArrayList<>();
                lore.add(Text.createText("&7Add a mob from MythicMobs").build(player));
                lore.add(Text.createText("&7(internal mob name)").build(player));
                lore.add("");
                lore.add(Text.createText("&eClick to add").build(player));
                mMeta.setLore(lore);
                mythic.setItemMeta(mMeta);
            }
            inventory.setItem(51, mythic);
            clickHandlers.put("51", p -> addMobFlow("MYTHIC"));
        } else {
            inventory.setItem(51, null);
        }
    }

    private void addMobFlow(String type) {
        close();
        String promptKey = type.equals("MYTHIC") ? "prompts.mythic-mob-id" : "prompts.vanilla-mob-id";

        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang(promptKey).build())
                .withValidator(InputValidators.notEmpty())
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-item-format").build())
                .onComplete(idInput -> {
                    String id = idInput.trim().toUpperCase();

                    // Validate vanilla entity type up front so a typo doesn't
                    // silently save a mob that can never spawn.
                    if (type.equals("VANILLA")) {
                        try {
                            org.bukkit.entity.EntityType.valueOf(id);
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(Text.createTextWithLang("prompts.invalid-mob-id").build(player));
                            open();
                            return;
                        }
                    }

                    plugin.getChatInputManager().requestInput(player)
                            .withPrompt(Text.createTextWithLang("prompts.mob-amount").build())
                            .withValidator(InputValidators.positiveInteger())
                            .withInvalidMessage(Text.createTextWithLang("prompts.invalid-number").build())
                            .onComplete(amountInput -> {
                                int amount = Integer.parseInt(amountInput.trim());

                                Map<String, Object> newMob = new LinkedHashMap<>();
                                newMob.put("type", type);
                                newMob.put("id", id);
                                newMob.put("amount", amount);
                                newMob.put("health-multiplier", 1.0);
                                newMob.put("damage-multiplier", 1.0);

                                List<Map<?, ?>> mobs = loadMobsList();
                                mobs.add(newMob);
                                saveMobsList(mobs);

                                player.sendMessage(Text.createTextWithLang("prompts.mob-added")
                                        .replace("{0}", id).build(player));
                                open();
                            })
                            .onCancel(this::open)
                            .start();
                })
                .onCancel(this::open)
                .start();
    }

    // ==========================================
    // DATA HELPERS
    // ==========================================

    private List<Map<?, ?>> loadMobsList() {
        return new ArrayList<>(plugin.getFileManager().getMobs().getMapList(wavePath + ".mobs"));
    }

    private void saveMobsList(List<Map<?, ?>> mobs) {
        plugin.getFileManager().getMobs().set(wavePath + ".mobs", mobs);
        plugin.getFileManager().getMobs().save();
    }

    private int toInt(Object value, int def) {
        return (value instanceof Number) ? ((Number) value).intValue() : def;
    }

    private double toDouble(Object value, double def) {
        return (value instanceof Number) ? ((Number) value).doubleValue() : def;
    }

    @Override
    protected void handleCustomAction(int slot, String actionType, String actionValue, String itemId) {
        switch (actionType) {
            case "spawn-delay":
                editSpawnDelay();
                break;
            case "mobs-per-spawn":
                editMobsPerSpawn();
                break;
            case "toggle-progression":
                boolean newManual = !isManualWaveProgression();
                plugin.getFileManager().getMobs().set(wavePath + ".progression", newManual ? "MANUAL" : "AUTO");
                plugin.getFileManager().getMobs().save();
                refresh();
                break;
            case "clear-mobs":
                saveMobsList(new ArrayList<>());
                player.sendMessage(Text.createTextWithLang("prompts.mobs-cleared").build(player));
                refresh();
                break;
            case "add-vanilla":
                addMobFlow("VANILLA");
                break;
            case "add-mythic":
                addMobFlow("MYTHIC");
                break;
            default:
                super.handleCustomAction(slot, actionType, actionValue, itemId);
                break;
        }
    }

    @Override
    protected void onBack() {
        new WaveEditorGUI(plugin, player, arena).open();
    }
}