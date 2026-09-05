package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import me.bixgamer707.hordes.utils.InputValidators;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
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
        buildInfoItem();
        buildSpawnDelayItem();
        buildMobsPerSpawnItem();
        buildProgressionItem();
        buildClearMobsItem();
        buildMobGrid();
        buildAddMobButtons();
    }

    // ==========================================
    // TOP CONTROL ROW
    // ==========================================

    private void buildInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.createText("&c&lWave " + waveNumber).build(player));
            List<String> lore = new ArrayList<>();
            lore.add(Text.createText("&7Arena: &e" + arena.getConfig().getDisplayName()).build(player));
            lore.add(Text.createText("&7Total waves: &e" + arena.getConfig().getTotalWaves()).build(player));
            lore.add("");
            lore.add(Text.createText("&7Left-click a mob to edit amount").build(player));
            lore.add(Text.createText("&7Shift-click a mob to edit multipliers").build(player));
            lore.add(Text.createText("&7Right-click a mob to remove it").build(player));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(4, item);
    }

    private void buildSpawnDelayItem() {
        int spawnDelay = plugin.getFileManager().getMobs().getInt(wavePath + ".spawn-delay", 20);

        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.createText("&e&lSpawn Delay").build(player));
            List<String> lore = new ArrayList<>();
            lore.add(Text.createText("&7Ticks between each mob batch").build(player));
            lore.add(Text.createText("&7(20 ticks = 1 second)").build(player));
            lore.add("");
            lore.add(Text.createText("&7Current: &e" + spawnDelay + " ticks").build(player));
            lore.add("");
            lore.add(Text.createText("&eClick to change").build(player));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(10, item);
        clickHandlers.put("10", p -> editSpawnDelay());
    }

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

    private void buildMobsPerSpawnItem() {
        int mobsPerSpawn = plugin.getFileManager().getMobs().getInt(wavePath + ".mobs-per-spawn", 1);

        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.createText("&e&lMobs Per Batch").build(player));
            List<String> lore = new ArrayList<>();
            lore.add(Text.createText("&7How many mobs spawn together").build(player));
            lore.add(Text.createText("&7every spawn-delay tick cycle").build(player));
            lore.add("");
            lore.add(Text.createText("&7Current: &e" + mobsPerSpawn).build(player));
            lore.add("");
            lore.add(Text.createText("&eClick to change").build(player));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(12, item);
        clickHandlers.put("12", p -> editMobsPerSpawn());
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

    private void buildProgressionItem() {
        boolean manual = "MANUAL".equalsIgnoreCase(
                plugin.getFileManager().getMobs().getString(wavePath + ".progression", "AUTO"));

        ItemStack item = new ItemStack(manual ? Material.LEVER : Material.REPEATER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.createText("&e&lWave Progression").build(player));
            List<String> lore = new ArrayList<>();
            lore.add(Text.createText("&7Only matters if the arena's own").build(player));
            lore.add(Text.createText("&7wave-progression is set to &eMIXED").build(player));
            lore.add("");
            lore.add(Text.createText("&7Current: &e" + (manual ? "MANUAL" : "AUTO")).build(player));
            lore.add("");
            lore.add(Text.createText("&eClick to toggle").build(player));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(14, item);
        clickHandlers.put("14", p -> {
            boolean newManual = !manual;
            plugin.getFileManager().getMobs().set(wavePath + ".progression", newManual ? "MANUAL" : "AUTO");
            plugin.getFileManager().getMobs().save();
            refresh();
        });
    }

    private void buildClearMobsItem() {
        List<Map<?, ?>> mobs = loadMobsList();

        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.createText("&c&lClear All Mobs").build(player));
            List<String> lore = new ArrayList<>();
            lore.add(Text.createText("&7Removes all &e" + mobs.size() + " &7mob entries").build(player));
            lore.add(Text.createText("&7from this wave").build(player));
            lore.add("");
            lore.add(Text.createText("&eClick to clear").build(player));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(16, item);
        clickHandlers.put("16", p -> {
            saveMobsList(new ArrayList<>());
            player.sendMessage(Text.createTextWithLang("prompts.mobs-cleared").build(player));
            refresh();
        });
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

        Material material = "MYTHIC".equalsIgnoreCase(type) ? Material.NETHER_STAR : vanillaSpawnEggOrFallback(id);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.createText("&e&l" + id).build(player));
            List<String> lore = new ArrayList<>();
            lore.add(Text.createText("&7Type: &e" + type).build(player));
            lore.add(Text.createText("&7Amount: &e" + amount).build(player));
            lore.add(Text.createText("&7Health x: &e" + healthMult).build(player));
            lore.add(Text.createText("&7Damage x: &e" + damageMult).build(player));
            lore.add("");
            lore.add(Text.createText("&eLeft-click: &7edit amount").build(player));
            lore.add(Text.createText("&eShift-click: &7edit multipliers").build(player));
            lore.add(Text.createText("&cRight-click: &7remove").build(player));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
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
    protected void onBack() {
        new WaveEditorGUI(plugin, player, arena).open();
    }
}