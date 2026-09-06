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
import java.util.List;

/**
 * Advanced item rewards manager: a visual grid of every configured item
 * reward (stored as "MATERIAL AMOUNT" strings, same format RewardManager
 * already parses) with click-to-edit-amount, click-to-remove, and two ways
 * to add new ones - typing a material name, or instantly grabbing whatever
 * the admin is currently holding.
 */
public class ItemRewardsGUI extends BaseGUI {

    private static final int[] ITEM_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    private final Arena arena;
    private final String arenaId;

    public ItemRewardsGUI(Hordes plugin, Player player, Arena arena) {
        super(plugin, player, "admin-item-rewards", java.util.Collections.singletonMap("arena_id", arena.getId()));
        this.arena = arena;
        this.arenaId = arena.getId();
    }

    @Override
    protected void buildDynamic() {
        buildInfoItem();
        buildItemGrid();
        buildAddButtons();
    }

    // ==========================================
    // INFO
    // ==========================================

    private void buildInfoItem() {
        List<String> items = loadItems();

        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.createText("&b&lItem Rewards").build(player));
            List<String> lore = new ArrayList<>();
            lore.add(Text.createText("&7Arena: &e" + arena.getConfig().getDisplayName()).build(player));
            lore.add(Text.createText("&7Configured items: &e" + items.size()).build(player));
            lore.add("");
            lore.add(Text.createText("&7Left-click an item to edit its amount").build(player));
            lore.add(Text.createText("&7Right-click an item to remove it").build(player));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(4, item);
    }

    // ==========================================
    // ITEM GRID
    // ==========================================

    private void buildItemGrid() {
        List<String> items = loadItems();

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int slot = ITEM_SLOTS[i];

            if (i >= items.size()) {
                inventory.setItem(slot, null);
                continue;
            }

            String[] parts = items.get(i).split(" ");
            String materialName = parts[0];
            int amount = parts.length > 1 ? parseIntSafe(parts[1], 1) : 1;

            Material material;
            try {
                material = Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                material = Material.BARRIER; // shows broken/invalid entries visibly instead of crashing
            }

            ItemStack display = new ItemStack(material);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Text.createText("&e&l" + materialName).build(player));
                List<String> lore = new ArrayList<>();
                lore.add(Text.createText("&7Amount: &e" + amount).build(player));
                lore.add("");
                lore.add(Text.createText("&eLeft-click: &7edit amount").build(player));
                lore.add(Text.createText("&cRight-click: &7remove").build(player));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot, display);

            final int itemIndex = i;
            clickHandlers.put(slot + "", p -> handleItemClick(itemIndex));
        }
    }

    private void handleItemClick(int itemIndex) {
        ClickType clickType = clickTypes.get(ITEM_SLOTS[itemIndex]);

        if (clickType == ClickType.RIGHT) {
            List<String> items = loadItems();
            if (itemIndex < items.size()) {
                String removed = items.remove(itemIndex);
                saveItems(items);
                player.sendMessage(Text.createTextWithLang("prompts.item-reward-removed")
                        .replace("{0}", removed).build(player));
            }
            refresh();
        } else {
            editItemAmount(itemIndex);
        }
    }

    private void editItemAmount(int itemIndex) {
        List<String> items = loadItems();
        if (itemIndex >= items.size()) return;

        String[] parts = items.get(itemIndex).split(" ");
        String materialName = parts[0];

        close();
        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.item-reward-amount").build())
                .withValidator(InputValidators.positiveInteger())
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-number").build())
                .onComplete(input -> {
                    int newAmount = Integer.parseInt(input.trim());

                    List<String> current = loadItems();
                    if (itemIndex < current.size()) {
                        current.set(itemIndex, materialName + " " + newAmount);
                        saveItems(current);
                        player.sendMessage(Text.createTextWithLang("prompts.item-reward-amount-updated")
                                .replace("{0}", String.valueOf(newAmount)).build(player));
                    }
                    open();
                })
                .onCancel(this::open)
                .start();
    }

    // ==========================================
    // ADD BUTTONS
    // ==========================================

    private void buildAddButtons() {
        // Add by typing a material name
        ItemStack addByName = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta nameMeta = addByName.getItemMeta();
        if (nameMeta != null) {
            nameMeta.setDisplayName(Text.createText("&a&l+ Add By Name").build(player));
            List<String> lore = new ArrayList<>();
            lore.add(Text.createText("&7Type the material name and amount").build(player));
            lore.add(Text.createText("&7(e.g. DIAMOND, then 64)").build(player));
            lore.add("");
            lore.add(Text.createText("&eClick to add").build(player));
            nameMeta.setLore(lore);
            addByName.setItemMeta(nameMeta);
        }
        inventory.setItem(49, addByName);
        clickHandlers.put("49", p -> addByNameFlow());

        // Add whatever is currently in the admin's hand
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean hasHeldItem = heldItem != null && heldItem.getType() != Material.AIR;

        ItemStack addFromHand = hasHeldItem ? heldItem.clone() : new ItemStack(Material.PLAYER_HEAD);
        ItemMeta handMeta = addFromHand.getItemMeta();
        if (handMeta != null) {
            handMeta.setDisplayName(Text.createText(hasHeldItem
                    ? "&a&l+ Add This Item (" + heldItem.getType().name() + ")"
                    : "&7+ Add From Hand (nothing held)").build(player));
            List<String> lore = new ArrayList<>();
            if (hasHeldItem) {
                lore.add(Text.createText("&7Instantly adds &e" + heldItem.getType().name()
                        + " x" + heldItem.getAmount() + " &7as a reward").build(player));
                lore.add(Text.createText("&7using the amount currently in your hand.").build(player));
                lore.add("");
                lore.add(Text.createText("&eClick to add").build(player));
            } else {
                lore.add(Text.createText("&7Hold an item in your main hand,").build(player));
                lore.add(Text.createText("&7then come back and click this.").build(player));
            }
            handMeta.setLore(lore);
            addFromHand.setItemMeta(handMeta);
        }
        inventory.setItem(51, addFromHand);

        if (hasHeldItem) {
            clickHandlers.put("51", p -> addFromHand(heldItem.getType().name(), heldItem.getAmount()));
        }
    }

    private void addByNameFlow() {
        close();
        plugin.getChatInputManager().requestInput(player)
                .withPrompt(Text.createTextWithLang("prompts.vanilla-mob-id").build())
                .withValidator(InputValidators.notEmpty())
                .withInvalidMessage(Text.createTextWithLang("prompts.invalid-item-format").build())
                .onComplete(materialInput -> {
                    String materialName = materialInput.trim().toUpperCase();

                    try {
                        Material.valueOf(materialName);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Text.createTextWithLang("prompts.invalid-material").build(player));
                        open();
                        return;
                    }

                    plugin.getChatInputManager().requestInput(player)
                            .withPrompt(Text.createTextWithLang("prompts.item-reward-amount").build())
                            .withValidator(InputValidators.positiveInteger())
                            .withInvalidMessage(Text.createTextWithLang("prompts.invalid-number").build())
                            .onComplete(amountInput -> {
                                addFromHand(materialName, Integer.parseInt(amountInput.trim()));
                            })
                            .onCancel(this::open)
                            .start();
                })
                .onCancel(this::open)
                .start();
    }

    private void addFromHand(String materialName, int amount) {
        List<String> items = loadItems();
        items.add(materialName + " " + amount);
        saveItems(items);

        player.sendMessage(Text.createTextWithLang("prompts.item-reward-added")
                .replace("{0}", materialName)
                .replace("{1}", String.valueOf(amount)).build(player));

        open();
    }

    // ==========================================
    // DATA HELPERS
    // ==========================================

    private List<String> loadItems() {
        return new ArrayList<>(arena.getConfig().getRewardConfig().getItems());
    }

    private void saveItems(List<String> items) {
        plugin.getFileManager().getArenas().set("arenas." + arenaId + ".rewards.items", items);
        plugin.getFileManager().getArenas().save();
        plugin.getArenaManager().reloadArenas();
    }

    private int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    protected void onBack() {
        Arena reloaded = plugin.getArenaManager().getArena(arenaId);
        new RewardEditorGUI(plugin, player, reloaded != null ? reloaded : arena).open();
    }
}