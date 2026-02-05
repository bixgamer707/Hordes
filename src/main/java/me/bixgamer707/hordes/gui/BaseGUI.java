package me.bixgamer707.hordes.gui;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.file.File;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Base GUI class - 100% configurable from guis.yml
 * Everything can be configured: positions, materials, amounts, CustomModelData, etc.
 */
public abstract class BaseGUI {

    protected final Hordes plugin;
    protected final Player player;
    protected Inventory inventory;
    protected final String guiId;
    protected final File guiConfig;

    // Track open GUIs
    private static final Map<UUID, BaseGUI> openGUIs = new ConcurrentHashMap<>();

    // Pagination
    protected int currentPage = 0;
    protected int maxPages = 0;

    // Click handlers registered by GUI ID and slot
    protected final Map<String, Consumer<Player>> clickHandlers = new HashMap<>();

    /**
     * Constructor - loads everything from guis.yml
     */
    public BaseGUI(Hordes plugin, Player player, String guiId) {
        this.plugin = plugin;
        this.player = player;
        this.guiId = guiId;
        this.guiConfig = plugin.getFileManager().getFile("guis.yml");

        // Load title and size from config
        String title = getConfigString("title", "&6Menu");
        int rows = getConfigInt("rows", 6);
        int size = rows * 9;

        this.inventory = Bukkit.createInventory(null, size,
                Text.createText(title).build(player));
    }

    /**
     * Opens the GUI
     */
    public void open() {
        // Close existing GUI
        BaseGUI existing = openGUIs.get(player.getUniqueId());
        if (existing != null) {
            existing.onClose();
        }

        // Build GUI from config
        buildFromConfig();

        // Open inventory
        player.openInventory(inventory);
        openGUIs.put(player.getUniqueId(), this);

        // Play sound
        playSound(getConfigString("sounds.open", "UI_BUTTON_CLICK"));
    }

    /**
     * Builds GUI entirely from guis.yml configuration
     */
    protected void buildFromConfig() {
        inventory.clear();
        clickHandlers.clear();

        // Get items section
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("guis." + guiId + ".items");

        if (itemsSection == null) {
            plugin.logWarning("No items defined for GUI: " + guiId);
            return;
        }

        // Load each item
        for (String itemId : itemsSection.getKeys(false)) {
            String itemPath = "guis." + guiId + ".items." + itemId;
            loadAndSetItem(itemId, itemPath);
        }

        // Call custom build for dynamic content
        buildDynamic();
    }

    /**
     * Loads an item from config and sets it in the GUI
     */
    protected void loadAndSetItem(String itemId, String itemPath) {
        // Get slot(s)
        Object slotObj = guiConfig.get(itemPath + ".slot");
        List<Integer> slots = new ArrayList<>();

        if (slotObj instanceof Integer) {
            slots.add((Integer) slotObj);
        } else if (slotObj instanceof List) {
            for (Object obj : (List<?>) slotObj) {
                if (obj instanceof Integer) {
                    slots.add((Integer) obj);
                }
            }
        } else if (slotObj instanceof String) {
            // Support for ranges like "0-8" or "all-border"
            String slotStr = (String) slotObj;
            slots.addAll(parseSlotString(slotStr));
        }

        if (slots.isEmpty()) {
            return;
        }

        // Create item
        ItemStack item = createItemFromConfig(itemPath);

        if (item == null) {
            return;
        }

        // Set in all slots
        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item);

                // Register click handler if defined
                String action = guiConfig.getString(itemPath + ".action");
                if (action != null) {
                    clickHandlers.put(slot + "", p -> handleAction(action, itemId, p));
                }
            }
        }
    }

    /**
     * Creates an ItemStack from config path
     */
    protected ItemStack createItemFromConfig(String path) {
        // Material (required)
        String materialName = guiConfig.getString(path + ".material");
        if (materialName == null) {
            return null;
        }

        // Support for player head
        if (materialName.equalsIgnoreCase("PLAYER_HEAD") || materialName.equalsIgnoreCase("SKULL")) {
            return createPlayerHead(path);
        }

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) {
            plugin.logWarning("Invalid material: " + materialName + " at " + path);
            return null;
        }

        // Amount
        int amount = guiConfig.getInt(path + ".amount", 1);

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        // Display name
        String name = guiConfig.getString(path + ".name");
        if (name != null) {
            meta.setDisplayName(Text.createText(name).build(player));
        }

        // Lore
        List<String> lore = guiConfig.getStringList(path + ".lore");
        if (!lore.isEmpty()) {
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                processedLore.add(Text.createText(line).build(player));
            }
            meta.setLore(processedLore);
        }

        // CustomModelData
        if (guiConfig.contains(path + ".custom-model-data")) {
            meta.setCustomModelData(guiConfig.getInt(path + ".custom-model-data"));
        }

        // Glow effect
        if (guiConfig.getBoolean(path + ".glow", false)) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Hide flags
        if (guiConfig.getBoolean(path + ".hide-attributes", true)) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }

        if (guiConfig.getBoolean(path + ".hide-enchants", false)) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Unbreakable
        if (guiConfig.getBoolean(path + ".unbreakable", false)) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a player head
     */
    protected ItemStack createPlayerHead(String path) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta == null) {
            return skull;
        }

        // Owner
        String owner = guiConfig.getString(path + ".skull-owner");
        if (owner != null) {
            // Support for placeholders like {player}
            owner = owner.replace("{player}", player.getName());
            meta.setOwner(owner);
        }

        // Display name
        String name = guiConfig.getString(path + ".name");
        if (name != null) {
            meta.setDisplayName(Text.createText(name).build(player));
        }

        // Lore
        List<String> lore = guiConfig.getStringList(path + ".lore");
        if (!lore.isEmpty()) {
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                processedLore.add(Text.createText(line).build(player));
            }
            meta.setLore(processedLore);
        }

        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * Parses slot strings like "0-8", "all", "border", etc.
     */
    protected List<Integer> parseSlotString(String slotStr) {
        List<Integer> slots = new ArrayList<>();
        int size = inventory.getSize();
        int rows = size / 9;

        switch (slotStr.toLowerCase()) {
            case "all":
                for (int i = 0; i < size; i++) {
                    slots.add(i);
                }
                break;

            case "border":
                // Top and bottom rows
                for (int i = 0; i < 9; i++) {
                    slots.add(i);
                    slots.add((rows - 1) * 9 + i);
                }
                // Sides
                for (int i = 1; i < rows - 1; i++) {
                    slots.add(i * 9);
                    slots.add(i * 9 + 8);
                }
                break;

            case "corners":
                slots.add(0);
                slots.add(8);
                slots.add((rows - 1) * 9);
                slots.add(size - 1);
                break;

            default:
                // Range like "0-8" or "10-15"
                if (slotStr.contains("-")) {
                    String[] parts = slotStr.split("-");
                    try {
                        int start = Integer.parseInt(parts[0].trim());
                        int end = Integer.parseInt(parts[1].trim());
                        for (int i = start; i <= end && i < size; i++) {
                            slots.add(i);
                        }
                    } catch (NumberFormatException e) {
                        plugin.logWarning("Invalid slot range: " + slotStr);
                    }
                }
                break;
        }

        return slots;
    }

    /**
     * Override this for dynamic content (pagination, player-specific items, etc.)
     */
    protected void buildDynamic() {
        // Override in subclasses
    }

    /**
     * Handles click on inventory
     */
    protected void handleClick(int slot, ClickType clickType) {
        // Check for registered handler
        Consumer<Player> handler = clickHandlers.get(slot + "");

        if (handler != null) {
            handler.accept(player);
        }
    }

    /**
     * Handles action strings from config
     */
    protected void handleAction(String action, String itemId, Player player) {
        if (action == null || action.isEmpty()) {
            return;
        }

        String[] parts = action.split(":", 2);
        String actionType = parts[0].toLowerCase();
        String actionValue = parts.length > 1 ? parts[1] : "";

        switch (actionType) {
            case "close":
                close();
                break;

            case "command":
                player.performCommand(actionValue.replace("{player}", player.getName()));
                break;

            case "console":
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        actionValue.replace("{player}", player.getName()));
                break;

            case "message":
                player.sendMessage(Text.createText(actionValue).build(player));
                break;

            case "sound":
                playSound(actionValue);
                break;

            case "gui":
                // Open another GUI
                openGUI(actionValue);
                break;

            case "back":
                onBack();
                break;

            case "refresh":
                refresh();
                break;

            case "next-page":
                nextPage();
                break;

            case "previous-page":
                previousPage();
                break;

            default:
                // Custom action - handle in subclass
                handleCustomAction(actionType, actionValue, itemId);
                break;
        }
    }

    /**
     * Override for custom actions
     */
    protected void handleCustomAction(String actionType, String actionValue, String itemId) {
        // Override in subclasses
    }

    /**
     * Opens another GUI by ID
     */
    protected void openGUI(String guiId) {
        // Override in subclasses or use GUIManager
    }

    /**
     * Refreshes the GUI
     */
    public void refresh() {
        buildFromConfig();
    }

    /**
     * Closes the GUI
     */
    public void close() {
        player.closeInventory();
    }

    /**
     * Called when GUI is closed
     */
    protected void onClose() {
        openGUIs.remove(player.getUniqueId());
    }

    /**
     * Called when back button is clicked
     */
    protected void onBack() {
        close();
    }

    /**
     * Next page
     */
    protected void nextPage() {
        if (currentPage < maxPages - 1) {
            currentPage++;
            playSound(getConfigString("sounds.page", "ITEM_BOOK_PAGE_TURN"));
            refresh();
        }
    }

    /**
     * Previous page
     */
    protected void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            playSound(getConfigString("sounds.page", "ITEM_BOOK_PAGE_TURN"));
            refresh();
        }
    }

    /**
     * Plays a sound
     */
    protected void playSound(String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_"));
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception e) {
            // Invalid sound
        }
    }

    /**
     * Gets a config string
     */
    protected String getConfigString(String path, String def) {
        return guiConfig.getString("guis." + guiId + "." + path, def);
    }

    protected List<String> getConfigStringList(String path) {
        return guiConfig.getStringList("guis." + guiId + "." + path);
    }

    /**
     * Gets a config int
     */
    protected int getConfigInt(String path, int def) {
        return guiConfig.getInt("guis." + guiId + "." + path, def);
    }

    /**
     * Gets a config boolean
     */
    protected boolean getConfigBoolean(String path, boolean def) {
        return guiConfig.getBoolean("guis." + guiId + "." + path, def);
    }

    /**
     * Sends a message from GUI config or global messages
     */
    protected void sendConfigMessage(String path, Object... replacements) {
        String message = guiConfig.getString("guis." + guiId + ".messages." + path);

        if (message == null) {
            // Fallback to global messages in en_us.yml
            message = plugin.getFileManager().getFile("messages/en_us.yml")
                    .getString("Messages." + path, path);
        }

        // Replace placeholders
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
        }

        player.sendMessage(Text.createText(message).build(player));
    }

    /**
     * Updates item lore by item ID from config
     */
    protected void updateItemLore(String itemId, String[] replacements) {
        String slotStr = getConfigString("items." + itemId + ".slot", null);
        if (slotStr != null) {
            try {
                int slot = Integer.parseInt(slotStr);
                updateItemLore(slot, replacements);
            } catch (NumberFormatException e) {
                // Slot pattern, skip for now
            }
        }
    }

    protected void updateItemLore(int slot, String[] replacements) {
        ItemStack item = inventory.getItem(slot);
        if (item == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }

        List<String> lore = meta.getLore();
        if (lore == null) {
            return;
        }

        List<String> updatedLore = new ArrayList<>();
        for (String line : lore) {
            for (int i = 0; i < replacements.length; i++) {
                line = line.replace("{" + i + "}", replacements[i]);
            }
            updatedLore.add(line);
        }

        meta.setLore(updatedLore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    // Static methods
    public static BaseGUI getOpenGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }

    public static boolean hasGUIOpen(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }

    public static void closeGUI(Player player) {
        BaseGUI gui = openGUIs.get(player.getUniqueId());
        if (gui != null) {
            gui.close();
        }
    }

    // Getters
    public Player getPlayer() {
        return player;
    }

    public Hordes getPlugin() {
        return plugin;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public String getGuiId() {
        return guiId;
    }
}