package me.bixgamer707.hordes.gui.admin;

import me.bixgamer707.hordes.Hordes;
import me.bixgamer707.hordes.arena.Arena;
import me.bixgamer707.hordes.arena.ArenaState;
import me.bixgamer707.hordes.gui.BaseGUI;
import me.bixgamer707.hordes.text.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Arena list GUI - 100% configurable
 * All text, materials, and layout from guis.yml
 */
public class ArenaListGUI extends BaseGUI {

    private final List<Arena> arenas;
    private static final int ARENAS_PER_PAGE = 28;

    private static final int[] ARENA_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public ArenaListGUI(Hordes plugin, Player player) {
        super(plugin, player, "admin-arena-list");

        this.arenas = new ArrayList<>(plugin.getArenaManager().getArenas().values());
        this.maxPages = (int) Math.ceil((double) arenas.size() / ARENAS_PER_PAGE);
        if (maxPages == 0) maxPages = 1;
    }

    @Override
    protected void buildDynamic() {
        int startIndex = currentPage * ARENAS_PER_PAGE;
        int endIndex = Math.min(startIndex + ARENAS_PER_PAGE, arenas.size());

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Arena arena = arenas.get(i);
            int slot = ARENA_SLOTS[slotIndex];

            ItemStack arenaItem = createArenaItem(arena);
            inventory.setItem(slot, arenaItem);

            final String arenaId = arena.getId();
            clickHandlers.put(slot + "", p -> handleArenaClick(p, arenaId));

            slotIndex++;
        }

        updatePaginationButtons();
    }

    /**
     * Creates arena item with data from config
     */
    private ItemStack createArenaItem(Arena arena) {
        // Get material based on state from config
        String materialKey = "guis."+guiId+".items.arena-item";
        String materialName = guiConfig.getString(materialKey+".material-" + (arena.getConfig().isEnabled() ? "enabled" : "disabled"), "GRAY_WOOL");
        Material material = Material.valueOf(materialName);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Get name template from config
            String nameTemplate = guiConfig.getString(materialKey+".name", "&e{arena_id}");
            String name = replacePlaceholders(nameTemplate, arena);
            meta.setDisplayName(Text.createText(name).build(player));

            // Get lore from config
            List<String> loreTemplate = guiConfig.getStringList(materialKey+".lore");
            List<String> lore = new ArrayList<>();

            for (String line : loreTemplate) {
                String processed = replacePlaceholders(line, arena);
                lore.add(Text.createText(processed).build(player));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Replaces placeholders with arena data
     */
    private String replacePlaceholders(String text, Arena arena) {
        return text
                .replace("{arena_id}", arena.getId())
                .replace("{display_name}", arena.getConfig().getDisplayName())
                .replace("{state}", arena.getState().name())
                .replace("{state_color}", getStateColor(arena.getState()))
                .replace("{current_players}", String.valueOf(arena.getPlayerCount()))
                .replace("{max_players}", String.valueOf(arena.getConfig().getMaxPlayers()))
                .replace("{total_waves}", String.valueOf(arena.getConfig().getTotalWaves()))
                .replace("{enabled}", arena.getConfig().isEnabled() ?
                        guiConfig.getString("guis."+guiId+".items.arena-item.enabled-text", "&a✔") :
                        guiConfig.getString("guis."+guiId+".items.arena-item.disabled-text", "&c✘"));
    }

    /**
     * Gets color for arena state from config
     */
    private String getStateColor(ArenaState state) {
        String key = "guis."+guiId+".items.arena-item.state-color-" + state.name().toLowerCase();
        return guiConfig.getString(key, "&7");
    }

    /**
     * Handles arena click
     */
    private void handleArenaClick(Player player, String arenaId) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            player.sendMessage(Text.createTextWithLang("admin.arena-not-found").build(player));
            return;
        }

        // All actions configured in guis.yml via click-actions
        // Default: left-click opens editor
        new ArenaEditorGUI(plugin, player, arena).open();
    }

    /**
     * Updates pagination buttons
     */
    private void updatePaginationButtons() {
        if (currentPage > 0) {
            updateItemLore(45, new String[]{
                    String.valueOf(currentPage + 1),
                    String.valueOf(maxPages)
            });
        } else {
            inventory.setItem(45, null);
        }

        if (currentPage < maxPages - 1) {
            updateItemLore(53, new String[]{
                    String.valueOf(currentPage + 1),
                    String.valueOf(maxPages)
            });
        } else {
            inventory.setItem(53, null);
        }
    }

    @Override
    protected void handleCustomAction(int slot, String actionType, String actionValue, String itemId) {
        if (actionType.equals("create-arena")) {
            new ArenaCreateGUI(plugin, player).open();
        }
    }

    @Override
    protected void onBack() {
        new AdminMainGUI(plugin, player).open();
    }
}